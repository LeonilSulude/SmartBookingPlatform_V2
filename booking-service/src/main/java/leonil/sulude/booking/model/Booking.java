package leonil.sulude.booking.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID resourceId; // Reference for serviceResource in catalog-service

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private String customerEmail;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** optimistic locking — prevents concurrent modification of the same booking */
    @Version
    private Long version;

    /** idempotency key — client-provided unique ID to prevent duplicate bookings on retry */
    @Column(unique = true)
    private String idempotencyKey;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
