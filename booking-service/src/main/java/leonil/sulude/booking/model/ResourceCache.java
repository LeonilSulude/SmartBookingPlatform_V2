package leonil.sulude.booking.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Local cache of stable resource data from the Catalog Service.
 * Populated on startup via OpenFeign and kept up to date via Kafka events.
 * Reduces synchronous calls to Catalog for data that rarely changes.
 */
@Entity
@Table(name = "resource_cache")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourceCache {

    @Id
    private UUID id; // same ID as the resource in the Catalog Service

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    private Integer durationInMinutes;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime lastUpdated; // tracks when the cache entry was last refreshed
}