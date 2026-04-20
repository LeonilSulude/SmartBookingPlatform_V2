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
import leonil.sulude.booking.repository.BookingRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingServiceImpl implements BookingService {

    private final BookingRepository repository;
    private final CatalogClient catalogClient;

    public BookingServiceImpl(BookingRepository repository, CatalogClient catalogClient) {
        this.repository = repository;
        this.catalogClient = catalogClient;
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

        // Check if there is conflict for the booked time
        boolean hasConflict = repository.existsOverlappingBooking(dto.resourceId(), dto.startTime(), dto.endTime());
        if (hasConflict) {
            throw new BookingConflictException("Resource is already booked during this time.");
        }

        // Retrieve resource from Catalog Service (protected by resilience patterns)
        ServiceResourceResponseDTO resource = fetchResource(dto.resourceId());

        if (resource == null || !resource.active()) {
            throw new ResourceUnavailableException("Service resource is not available for booking");
        }

        // Check if the reservation conflicts with periods of unavailability
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

        ServiceResourceResponseDTO resource = fetchResource(booking.getResourceId());

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