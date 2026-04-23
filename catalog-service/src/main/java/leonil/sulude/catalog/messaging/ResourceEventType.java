package leonil.sulude.catalog.messaging;

/**
 * Event types published to Kafka when a resource lifecycle changes.
 * Consumed by the Booking Service to maintain a local resource cache.
 */
public enum ResourceEventType {
    RESOURCE_CREATED,
    RESOURCE_UPDATED,
    RESOURCE_DEACTIVATED
}