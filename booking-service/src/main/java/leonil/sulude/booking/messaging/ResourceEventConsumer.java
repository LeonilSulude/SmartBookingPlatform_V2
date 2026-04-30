package leonil.sulude.booking.messaging;

import leonil.sulude.booking.dto.ResourceCacheEventDTO;
import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Consumes resource lifecycle events from Kafka and maintains the local resource cache.
 * Handles RESOURCE_CREATED, RESOURCE_UPDATED, RESOURCE_ACTIVATED, and RESOURCE_DEACTIVATED.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceEventConsumer {

    private final ResourceCacheRepository resourceCacheRepository;

    @KafkaListener(topics = "catalog.resource.events", groupId = "booking-service")
    public void consume(ResourceCacheEventDTO event) {
        log.info("Received resource event [{}] for resource [{}]", event.eventType(), event.resourceId());

        switch (event.eventType()) {
            case "RESOURCE_CREATED", "RESOURCE_UPDATED", "RESOURCE_ACTIVATED" -> upsert(event);
            case "RESOURCE_DEACTIVATED" -> deactivate(event);
            default -> log.warn("Unknown resource event type [{}] — ignoring", event.eventType());
        }
    }

    /**
     * Creates or updates a cache entry with the latest stable resource data.
     */
    private void upsert(ResourceCacheEventDTO event) {
        ResourceCache cache = resourceCacheRepository.findById(event.resourceId())
                .orElse(new ResourceCache());

        cache.setId(event.resourceId());
        cache.setName(event.name());
        cache.setPrice(event.price());
        cache.setDurationInMinutes(event.durationInMinutes());
        cache.setActive(event.active());
        cache.setLastUpdated(LocalDateTime.now());

        resourceCacheRepository.save(cache);
        log.info("Resource cache updated for resource [{}]", event.resourceId());
    }

    /**
     * Marks a resource as inactive in the cache — prevents new bookings without removing history.
     */
    private void deactivate(ResourceCacheEventDTO event) {
        resourceCacheRepository.findById(event.resourceId())
                .ifPresentOrElse(
                        cache -> {
                            cache.setActive(false);
                            cache.setLastUpdated(LocalDateTime.now());
                            resourceCacheRepository.save(cache);
                            log.info("Resource [{}] marked as inactive in cache", event.resourceId());
                        },
                        () -> log.warn("Received RESOURCE_DEACTIVATED for unknown resource [{}]", event.resourceId())
                );
    }
}