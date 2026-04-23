package leonil.sulude.catalog.dto;

import leonil.sulude.catalog.messaging.ResourceEventType;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Event published to Kafka when a resource is created, updated, or deactivated.
 * Consumed by the Booking Service to maintain a local cache of stable resource data.
 */
public record ResourceEventDTO(
        ResourceEventType eventType,
        UUID resourceId,
        String name,
        BigDecimal price,
        Integer durationInMinutes,
        boolean active
) {}