package leonil.sulude.booking.integration;

import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for ResourceEventConsumer, publishing real Kafka messages to
 * "catalog.resource.events" and verifying the local resource_cache is updated correctly
 * for each event type. This is the core of the Kafka hybrid model's consumer side ---
 * previously covered only indirectly through OpenFeign cache-miss paths in other tests,
 * which never actually exercised the @KafkaListener itself. Flagged by a 3% JaCoCo
 * coverage reading on the messaging package.
 */
class ResourceEventConsumerIT extends AbstractIntegrationTest {

    private static final String TOPIC = "catalog.resource.events";

    @Autowired
    private ResourceCacheRepository resourceCacheRepository;

    @Test
    void shouldUpsertCacheOnResourceCreatedEvent() {
        UUID resourceId = UUID.randomUUID();
        String payload = """
                {
                    "eventType": "RESOURCE_CREATED",
                    "resourceId": "%s",
                    "name": "Consumer Test Resource",
                    "price": 22.50,
                    "durationInMinutes": 40,
                    "active": true
                }
                """.formatted(resourceId);

        publishMessage(TOPIC, resourceId.toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResourceCache cache = resourceCacheRepository.findById(resourceId).orElseThrow();
            assertThat(cache.getName()).isEqualTo("Consumer Test Resource");
            assertThat(cache.getPrice()).isEqualByComparingTo(java.math.BigDecimal.valueOf(22.50));
            assertThat(cache.isActive()).isTrue();
        });
    }

    @Test
    void shouldUpdateCacheOnResourceUpdatedEvent() {
        UUID resourceId = UUID.randomUUID();
        // pre-populate the cache — simulates a resource that was already created earlier
        resourceCacheRepository.save(ResourceCache.builder()
                .id(resourceId).name("Old Name").price(java.math.BigDecimal.valueOf(10.00))
                .durationInMinutes(30).active(true).lastUpdated(LocalDateTime.now()).build());

        String payload = """
                {
                    "eventType": "RESOURCE_UPDATED",
                    "resourceId": "%s",
                    "name": "New Name",
                    "price": 99.99,
                    "durationInMinutes": 90,
                    "active": true
                }
                """.formatted(resourceId);

        publishMessage(TOPIC, resourceId.toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResourceCache cache = resourceCacheRepository.findById(resourceId).orElseThrow();
            assertThat(cache.getName()).isEqualTo("New Name");
            assertThat(cache.getPrice()).isEqualByComparingTo(java.math.BigDecimal.valueOf(99.99));
            assertThat(cache.getDurationInMinutes()).isEqualTo(90);
        });
    }

    @Test
    void shouldMarkInactiveOnResourceDeactivatedEvent() {
        UUID resourceId = UUID.randomUUID();
        resourceCacheRepository.save(ResourceCache.builder()
                .id(resourceId).name("Active Resource").price(java.math.BigDecimal.valueOf(15.00))
                .durationInMinutes(30).active(true).lastUpdated(LocalDateTime.now()).build());

        String payload = """
                {
                    "eventType": "RESOURCE_DEACTIVATED",
                    "resourceId": "%s",
                    "name": "Active Resource",
                    "price": 15.00,
                    "durationInMinutes": 30,
                    "active": false
                }
                """.formatted(resourceId);

        publishMessage(TOPIC, resourceId.toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResourceCache cache = resourceCacheRepository.findById(resourceId).orElseThrow();
            assertThat(cache.isActive()).isFalse();
        });
    }

    @Test
    void shouldMarkActiveOnResourceActivatedEvent() {
        UUID resourceId = UUID.randomUUID();
        resourceCacheRepository.save(ResourceCache.builder()
                .id(resourceId).name("Reactivated Resource").price(java.math.BigDecimal.valueOf(12.00))
                .durationInMinutes(20).active(false).lastUpdated(LocalDateTime.now()).build());

        String payload = """
                {
                    "eventType": "RESOURCE_ACTIVATED",
                    "resourceId": "%s",
                    "name": "Reactivated Resource",
                    "price": 12.00,
                    "durationInMinutes": 20,
                    "active": true
                }
                """.formatted(resourceId);

        publishMessage(TOPIC, resourceId.toString(), payload);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResourceCache cache = resourceCacheRepository.findById(resourceId).orElseThrow();
            assertThat(cache.isActive()).isTrue();
        });
    }

    @Test
    void shouldIgnoreDeactivatedEventForUnknownResourceWithoutCreatingGhostEntry() {
        // a RESOURCE_DEACTIVATED event for a resourceId never seen before — the consumer's
        // ifPresentOrElse should log a warning and do nothing, never creating a new entry
        UUID unknownResourceId = UUID.randomUUID();
        String payload = """
                {
                    "eventType": "RESOURCE_DEACTIVATED",
                    "resourceId": "%s",
                    "name": "Never Existed",
                    "price": 1.00,
                    "durationInMinutes": 10,
                    "active": false
                }
                """.formatted(unknownResourceId);

        publishMessage(TOPIC, unknownResourceId.toString(), payload);

        // give the consumer time to process, then confirm no entry was ever created
        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(resourceCacheRepository.findById(unknownResourceId)).isEmpty());
    }
}