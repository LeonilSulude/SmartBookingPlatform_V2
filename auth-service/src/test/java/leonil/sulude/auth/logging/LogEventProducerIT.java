package leonil.sulude.auth.logging;

import leonil.sulude.auth.integration.AbstractIntegrationTest;
import leonil.sulude.auth.logging.dto.LogEventMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration test for LogEventProducer, using a real RabbitMQ broker via Testcontainers.
 * See catalog-service's LogEventProducerIT for the full rationale — same pattern here.
 *
 * Extends AbstractIntegrationTest to reuse the shared Postgres singleton container,
 * and adds its own RabbitMQ container following the same singleton pattern.
 *
 * IT suffix (not Test) --- needs a real Testcontainers RabbitMQ broker (plus Postgres via
 * AbstractIntegrationTest), so it must run under Failsafe (mvn verify), not Surefire
 * (mvn test), like every other integration test in this service.
 *
 * Note: as with the other services, LogEventProducer.send() is never actually invoked
 * from any business code path in the Auth Service — see the report for this documented
 * gap and the V3 direction (consolidating audit logging as an additional Kafka consumer
 * group on existing business topics, removing RabbitMQ entirely).
 */
class LogEventProducerIT extends AbstractIntegrationTest {

    static final RabbitMQContainer rabbitMQ;

    static {
        rabbitMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
        rabbitMQ.start();
    }

    @DynamicPropertySource
    static void configureRabbitMQProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

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

        assertDoesNotThrow(() -> producer.send(event));
    }
}
