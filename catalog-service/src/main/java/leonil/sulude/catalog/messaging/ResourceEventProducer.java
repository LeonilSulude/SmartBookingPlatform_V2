package leonil.sulude.catalog.messaging;

import leonil.sulude.catalog.dto.ResourceEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes resource lifecycle events to Kafka.
 * The Booking Service consumes these events to maintain a local cache of stable resource data.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResourceEventProducer {

    private static final String TOPIC = "catalog.resource.events";

    private final KafkaTemplate<String, ResourceEventDTO> kafkaTemplate;

    /**
     * Publishes a resource event to Kafka.
     * Uses resourceId as the message key — guarantees ordering per resource.
     *
     * @param event the resource event to publish
     */
    public void publish(ResourceEventDTO event) {
        kafkaTemplate.send(TOPIC, event.resourceId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish resource event [{}] for resource [{}]: {}",
                                event.eventType(), event.resourceId(), ex.getMessage());
                    } else {
                        log.info("Published resource event [{}] for resource [{}]",
                                event.eventType(), event.resourceId());
                    }
                });
    }
}