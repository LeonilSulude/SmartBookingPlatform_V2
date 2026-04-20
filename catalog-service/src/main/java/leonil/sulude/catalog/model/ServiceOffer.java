package leonil.sulude.catalog.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_offers")
@Data
@NoArgsConstructor
public class ServiceOffer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceCategory category;

    @Column(nullable = false)
    private String providerName;

    private String location;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Transient
    private List<ServiceResource> resources;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public ServiceOffer(String title, String description, ServiceCategory category,
                        String providerName, String location) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.providerName = providerName;
        this.location = location;
    }

}

