package leonil.sulude.booking.startup;

import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.feignclient.CatalogClient;
import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the local resource cache on startup by fetching all active resources from the Catalog Service.
 * Retries up to 3 times with exponential backoff if the Catalog Service is unavailable.
 * If all retries fail, the system starts with an empty cache — the OpenFeign fallback in
 * BookingServiceImpl ensures correctness via cache miss handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceCacheSeeder {

    private final CatalogClient catalogClient;
    private final ResourceCacheRepository resourceCacheRepository;

    /**
     * Triggered after the application is fully started.
     * Uses ApplicationReadyEvent instead of @PostConstruct to ensure
     * Eureka registration and Feign clients are ready before calling Catalog Service.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedResourceCache() {
        log.info("Seeding resource cache from Catalog Service...");
        attemptSeed();
    }

    /**
     * Attempts to seed the cache with retry support.
     * Retries up to 3 times with exponential backoff (1s, 2s, 4s) on any exception.
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)  // 1s, 2s, 4s
    )
    public void attemptSeed() {
        List<ServiceResourceResponseDTO> resources = catalogClient.getAllActiveResources();

        List<ResourceCache> cacheEntries = resources.stream()
                .map(r -> ResourceCache.builder()
                        .id(r.id())
                        .name(r.name())
                        .price(r.price())
                        .durationInMinutes(r.durationInMinutes())
                        .active(r.active())
                        .lastUpdated(LocalDateTime.now())
                        .build())
                .toList();

        resourceCacheRepository.saveAll(cacheEntries);
        log.info("Resource cache seeded successfully with {} resources", cacheEntries.size());
    }

    /**
     * Fallback executed when all retry attempts are exhausted.
     * System starts with empty cache — BookingServiceImpl handles cache misses
     * via OpenFeign fallback, ensuring correctness at the cost of performance.
     */
    @Recover
    public void recover(Exception ex) {
        log.warn("Failed to seed resource cache after all retries: {}. " +
                        "Starting with empty cache — cache will be populated incrementally via Kafka and OpenFeign fallback.",
                ex.getMessage());
    }
}