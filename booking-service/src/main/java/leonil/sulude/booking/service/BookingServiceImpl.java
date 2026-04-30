package leonil.sulude.booking.service;

import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.exception.BookingConflictException;
import leonil.sulude.booking.exception.ResourceUnavailableException;
import leonil.sulude.booking.feignclient.CatalogClient;
import leonil.sulude.booking.model.Booking;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.BookingRepository;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repository;
    private final CatalogClient catalogClient;
    private final ResourceCacheRepository resourceCacheRepository; // local cache of stable resource data from Catalog

    public BookingServiceImpl(BookingRepository repository,
                              CatalogClient catalogClient,
                              ResourceCacheRepository resourceCacheRepository) {
        this.repository = repository;
        this.catalogClient = catalogClient;
        this.resourceCacheRepository = resourceCacheRepository;
    }

    @Override
    public List<BookingResponseDTO> getAll() {
        return repository.findAll()
                .stream().map(this::mapToResponseDTO).toList();
    }

    @Override
    public Optional<BookingResponseDTO> getById(UUID id) {
        return repository.findById(id)
                .map(this::mapToResponseDTO);
    }

    @Override
    public BookingResponseDTO create(BookingRequestDTO dto) {

        // if client provided an idempotency key, check if this request was already processed
        if (dto.idempotencyKey() != null && !dto.idempotencyKey().isBlank()) {
            Optional<Booking> existing = repository.findByIdempotencyKey(dto.idempotencyKey());
            if (existing.isPresent()) {
                // return the original response without creating a duplicate
                return mapToResponseDTO(existing.get());
            }
        }

        // Check if there is conflict for the booked time
        boolean hasConflict = repository.existsOverlappingBooking(
                dto.resourceId(), dto.startTime(), dto.endTime());
        if (hasConflict) {
            throw new BookingConflictException("Resource is already booked during this time.");
        }

        // resolve resource — local cache first, fallback to Catalog via OpenFeign on cache miss
        ServiceResourceResponseDTO resource = resolveResource(dto.resourceId());

        if (resource == null || !resource.active()) {
            throw new ResourceUnavailableException("Service resource is not available for booking");
        }

        // Check if the reservation conflicts with periods of unavailability
        // unavailablePeriods always come from Catalog — time-sensitive, cannot tolerate staleness
        boolean unavailableConflict = resource.unavailablePeriods() != null &&
                resource.unavailablePeriods().stream().anyMatch(period ->
                        dto.startTime().isBefore(period.endTime()) &&
                                dto.endTime().isAfter(period.startTime())
                );

        if (unavailableConflict) {
            throw new ResourceUnavailableException("Resource is unavailable during the selected time.");
        }

        Booking booking = new Booking();
        booking.setResourceId(dto.resourceId());
        booking.setCustomerName(dto.customerName());
        booking.setCustomerEmail(dto.customerEmail());
        booking.setStartTime(dto.startTime());
        booking.setEndTime(dto.endTime());
        booking.setStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setIdempotencyKey(dto.idempotencyKey()); // persist key to detect future retries

        Booking saved = repository.save(booking);

        return new BookingResponseDTO(
                saved.getId(),
                saved.getResourceId(),
                saved.getCustomerName(),
                saved.getCustomerEmail(),
                saved.getStartTime(),
                saved.getEndTime(),
                saved.getStatus(),
                saved.getCreatedAt(),
                resource.name(),
                resource.price(),
                resource.durationInMinutes()
        );
    }

    @Override
    public boolean delete(UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }

    private BookingResponseDTO mapToResponseDTO(Booking booking) {
        // use cache for stable data — avoids Catalog call for every booking in list
        ServiceResourceResponseDTO resource = resolveResource(booking.getResourceId());

        return new BookingResponseDTO(
                booking.getId(),
                booking.getResourceId(),
                booking.getCustomerName(),
                booking.getCustomerEmail(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus(),
                booking.getCreatedAt(),
                resource != null ? resource.name() : null,
                resource != null ? resource.price() : null,
                resource != null ? resource.durationInMinutes() : null
        );
    }

    /**
     * Resolves resource data using a two-tier strategy:
     * 1. Local resource cache — fast, no network call, covers stable data (name, price, duration)
     * 2. Catalog Service via OpenFeign — fallback for cache misses, also populates the cache
     *
     * Unavailable periods are always fetched from Catalog — they are time-sensitive
     * and cannot tolerate staleness without risking double-booking.
     */
    private ServiceResourceResponseDTO resolveResource(UUID resourceId) {
        // check local cache first
        Optional<ResourceCache> cached = resourceCacheRepository.findById(resourceId);

        if (cached.isPresent()) {
            ResourceCache cache = cached.get();
            // fetch unavailable periods from Catalog — time-sensitive, must be real-time
            ServiceResourceResponseDTO fromCatalog = fetchResource(resourceId);
            return new ServiceResourceResponseDTO(
                    cache.getId(),
                    cache.getName(),
                    cache.getPrice(),
                    cache.getDurationInMinutes(),
                    cache.isActive(),
                    fromCatalog != null ? fromCatalog.unavailablePeriods() : List.of()
            );
        }

        // cache miss — fetch everything from Catalog and populate cache for future requests
        ServiceResourceResponseDTO resource = fetchResource(resourceId);
        if (resource != null) {
            ResourceCache newEntry = ResourceCache.builder()
                    .id(resource.id())
                    .name(resource.name())
                    .price(resource.price())
                    .durationInMinutes(resource.durationInMinutes())
                    .active(resource.active())
                    .lastUpdated(LocalDateTime.now())
                    .build();
            resourceCacheRepository.save(newEntry);
        }
        return resource;
    }

    /**
     * Retrieves resource data from the Catalog Service.
     * Protected by Resilience4j retry and circuit breaker mechanisms
     * to improve reliability of inter-service communication.
     */
    @CircuitBreaker(name = "catalogService", fallbackMethod = "catalogFallback")
    @Retry(name = "catalogService")
    public ServiceResourceResponseDTO fetchResource(UUID resourceId) {
        return catalogClient.getResourceById(resourceId);
    }

    /**
     * Fallback method executed when the Catalog Service is unavailable
     * after retries or when the circuit breaker is open.
     */
    public ServiceResourceResponseDTO catalogFallback(UUID resourceId, Throwable ex) {
        throw new ResourceUnavailableException("Catalog service unavailable");
    }
}