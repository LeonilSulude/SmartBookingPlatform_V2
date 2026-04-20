package leonil.sulude.booking.logging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEventMessage {

    private String correlationId; // ID used across the app
    private String serviceName;   // e.g. auth-service
    private String eventType;     // USER_REGISTERED, LOGIN_FAILED, etc.
    private String level;         // INFO, WARN, ERROR
    private String message;       // Human-readable log
    private Instant timestamp;    // When the event occurred
}

