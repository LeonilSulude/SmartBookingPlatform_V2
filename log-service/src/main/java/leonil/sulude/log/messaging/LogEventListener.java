package leonil.sulude.log.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.log.domain.LogEvent;
import leonil.sulude.log.dto.LogEventMessage;
import leonil.sulude.log.repository.LogEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventListener {

    private final LogEventRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Consumes log events from RabbitMQ.
     * This method is triggered asynchronously whenever a message
     * arrives in the configured queue.
     */
    @RabbitListener(queues = RabbitMQConstants.LOG_QUEUE)
    public void handleLogEvent(String message) {
        try {
            // Convert JSON message into DTO
            LogEventMessage event =
                    objectMapper.readValue(message, LogEventMessage.class);

            // --- CorrelationId fallback logic ---
            String correlationId;
            String source;

            if (event.getCorrelationId() == null || event.getCorrelationId().isBlank()) {
                correlationId = UUID.randomUUID().toString();
                source = "SYSTEM"; // Message not coming from an HTTP request
            } else {
                correlationId = event.getCorrelationId();
                source = "HTTP"; // Propagated from API Gateway / request flow
            }

            // Map DTO to persistence entity
            LogEvent logEvent = LogEvent.builder()
                    .serviceName(event.getServiceName())
                    .eventType(event.getEventType())
                    .level(event.getLevel())
                    .message(event.getMessage())
                    .correlationId(correlationId)
                    .source(source)
                    .createdAt(
                            event.getTimestamp() != null
                                    ? event.getTimestamp()
                                    : Instant.now()
                    )
                    .build();

            // Persist log event
            repository.save(logEvent);

            log.info(
                    "Log stored | service={} | level={} | event={} | correlationId={}",
                    event.getServiceName(),
                    event.getLevel(),
                    event.getEventType(),
                    correlationId
            );

        } catch (Exception e) {
            // Errors are logged but do not stop the consumer
            log.error("Failed to process log message: {}", message, e);
        }
    }

}

