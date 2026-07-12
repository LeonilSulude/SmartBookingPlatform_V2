package leonil.sulude.catalog.logging;

import leonil.sulude.catalog.integration.AbstractIntegrationTest;
import leonil.sulude.catalog.logging.dto.LogEventMessage;
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
 * Integration test for LogEventProducer, using a real RabbitMQ broker via Testcontainers
 * instead of relying on infrastructure being manually started (the V1-era limitation:
 * "Infrastructure tests require Docker" — this test now brings its own Docker-managed
 * broker automatically, so it works identically on a local machine or in CI).
 *
 * Extends AbstractIntegrationTest to reuse the shared Postgres/Kafka singleton containers
 * (the full application context needs a working datasource and Kafka bean regardless of
 * what this specific test exercises) and adds its own RabbitMQ container following the
 * same singleton pattern — started once in a static block, never stopped explicitly.
 *
 * IT suffix (not Test) --- needs a real Testcontainers RabbitMQ broker (plus Postgres/Kafka
 * via AbstractIntegrationTest), so it must run under Failsafe (mvn verify), not Surefire
 * (mvn test), like every other integration test in this service.
 *
 * Note: LogEventProducer.send() is never actually invoked from any business code path
 * in this service (nor was it in V1) — this test only proves the publish mechanism itself
 * works. See the report for this documented gap and the V3 direction (consolidating
 * audit logging as an additional Kafka consumer group on the existing business topics,
 * removing RabbitMQ entirely).
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
                .serviceName("catalog-service")
                .eventType("TEST_EVENT")
                .level("INFO")
                .message("This is a catalog log event")
                .correlationId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .build();

        // publishes against a real RabbitMQ broker — not just "no exception was thrown
        // against a mock", but a genuine publish to a real exchange
        assertDoesNotThrow(() -> producer.send(event));
    }
}
