package leonil.sulude.log.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "log_event",
        indexes = {
                @Index(name = "idx_log_correlation_id", columnList = "correlationId") //Use index for faster queries
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogEvent {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String correlationId;
    private String serviceName;
    private String eventType;
    private String level;
    private String source;


    @Column(length = 2000) //To set as VARCHAR(2000)
    private String message;

    private Instant createdAt;
}

