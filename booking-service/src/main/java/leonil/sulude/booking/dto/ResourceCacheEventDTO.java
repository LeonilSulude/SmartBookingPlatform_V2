package leonil.sulude.booking.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO for Kafka events published by the Catalog Service.
 * Maps to ResourceEventDTO on the producer side — kept separate to avoid cross-service coupling.
 */
public record ResourceCacheEventDTO(
        String eventType,        // RESOURCE_CREATED, RESOURCE_UPDATED, RESOURCE_ACTIVATED, RESOURCE_DEACTIVATED
        UUID resourceId,
        String name,
        BigDecimal price,
        Integer durationInMinutes,
        boolean active
) {}