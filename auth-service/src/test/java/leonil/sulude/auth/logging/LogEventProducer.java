package leonil.sulude.auth.logging;

import leonil.sulude.auth.logging.dto.LogEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class LogEventProducerTest {

    @Autowired
    private LogEventProducer producer;

    @Test
    void shouldPublishLogEventToRabbitMQ() {
        LogEventMessage event = LogEventMessage.builder()
                .serviceName("auth-service")
                .eventType("TEST_EVENT")
                .level("INFO")
                .message("This is a test log event")
                .correlationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();

        // Should not throw any exception
        assertDoesNotThrow(() -> producer.send(event));
    }
}

