package leonil.sulude.booking.startup;

import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.feignclient.CatalogClient;
import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the local resource cache on startup by fetching all active resources from the Catalog Service.
 * This ensures the cache is populated before the first booking request arrives.
 * After startup, the cache is kept up to date via Kafka events.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceCacheSeeder {

    private final CatalogClient catalogClient;
    private final ResourceCacheRepository resourceCacheRepository;

    /**
     * Triggered after the application is fully started and ready to serve requests.
     * Fetches all active resources from Catalog and populates the local cache.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedResourceCache() {
        log.info("Seeding resource cache from Catalog Service...");

        try {
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
            log.info("Resource cache seeded with {} resources", cacheEntries.size());

        } catch (Exception ex) {
            // log and continue — cache miss fallback to OpenFeign handles individual requests
            log.warn("Failed to seed resource cache on startup: {}. Cache will be populated incrementally via Kafka.", ex.getMessage());
        }
    }
}