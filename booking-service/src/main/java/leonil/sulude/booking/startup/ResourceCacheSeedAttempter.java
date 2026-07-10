package leonil.sulude.booking.startup;

import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.feignclient.CatalogClient;
import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Performs the actual cache-seeding attempt, with Spring Retry.
 *
 * This MUST be a separate bean from ResourceCacheSeeder, not a method called via
 * self-invocation from within it. Spring implements @Retryable/@Recover via a dynamic
 * proxy wrapped around the bean --- the proxy only intercepts calls arriving from OUTSIDE
 * the bean. When seedResourceCache() called this.attemptSeed() as a method in the same
 * class, the proxy was bypassed entirely: no retries ever happened, @Recover was never
 * invoked, and the raw exception propagated out of the ApplicationReadyEvent listener,
 * failing the entire application context on any Catalog outage at startup --- discovered
 * via ResourceCacheSeederFailureIT, which simulates exactly this scenario.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceCacheSeedAttempter {

    private final CatalogClient catalogClient;
    private final ResourceCacheRepository resourceCacheRepository;

    /**
     * Attempts to seed the cache. Retries up to 3 times with exponential backoff (1s, 2s, 4s).
     */
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
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
     * System starts with an empty cache — BookingServiceImpl handles cache misses
     * via CatalogResourceClient fallback, ensuring correctness at the cost of performance.
     */
    @Recover
    public void recover(Exception ex) {
        log.warn("Failed to seed resource cache after all retries: {}. " +
                        "Starting with empty cache — cache will be populated incrementally via Kafka and Catalog fallback.",
                ex.getMessage());
    }
}