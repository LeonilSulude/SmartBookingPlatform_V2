package leonil.sulude.catalog.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.logging.dto.LogEventMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventProducer {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void send(LogEventMessage event) {
        try {
            // Retrieve correlation ID from MDC
            String correlationId = MDC.get("correlationId");

            event.setCorrelationId(correlationId);

            // Ensure timestamp exists
            if (event.getTimestamp() == null) {
                event.setTimestamp(Instant.now());
            }

            String payload = objectMapper.writeValueAsString(event);

            rabbitTemplate.convertAndSend(
                    RabbitMQConstants.LOG_EXCHANGE,
                    RabbitMQConstants.LOG_ROUTING_KEY,
                    payload
            );


            log.debug(
                    "Log event sent | service={} | level={} | event={}",
                    event.getServiceName(),
                    event.getLevel(),
                    event.getEventType()
            );

        } catch (Exception e) {
            // Logging failures must never break business logic
            log.error("Failed to publish log event", e);
        }
    }
}

