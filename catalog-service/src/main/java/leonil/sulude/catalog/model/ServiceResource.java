package leonil.sulude.catalog.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "service_resources")
@Data
@NoArgsConstructor
public class ServiceResource {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(optional = false)
    private ServiceOffer offer;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private BigDecimal price;

    private Integer durationInMinutes;

    @Column(nullable = false)
    private boolean active = true;

    @ElementCollection
    @CollectionTable(
            name = "unavailable_periods",
            joinColumns = @JoinColumn(name = "resource_id")
    )
    private List<UnavailablePeriod> unavailablePeriods = new ArrayList<>();
}
