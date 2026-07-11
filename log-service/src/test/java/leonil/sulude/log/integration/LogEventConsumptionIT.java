package leonil.sulude.log.integration;

import leonil.sulude.log.domain.LogEvent;
import leonil.sulude.log.repository.LogEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for LogEventListener, publishing real messages to a real RabbitMQ
 * broker via Testcontainers and verifying they are correctly persisted in PostgreSQL.
 *
 * Unlike a unit test with a mocked repository, this exercises the full asynchronous
 * path: RabbitTemplate publish -> real broker -> @RabbitListener delivery -> JSON
 * deserialization -> entity mapping -> real database write. Because message delivery
 * is asynchronous, assertions use Awaitility to poll rather than asserting immediately
 * after publish.
 */
class LogEventConsumptionIT extends AbstractIntegrationTest {

	private static final String LOG_EXCHANGE = "app.logs.exchange";
	private static final String ROUTING_KEY = "app.logs.test-service";

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private LogEventRepository logEventRepository;

	@Test
	void shouldPersistLogEventWithHttpSourceWhenCorrelationIdIsPresent() {
		String correlationId = UUID.randomUUID().toString();
		String payload = """
                {
                    "correlationId": "%s",
                    "serviceName": "test-service",
                    "eventType": "USER_REGISTERED",
                    "level": "INFO",
                    "message": "Test user registered",
                    "timestamp": "2026-07-11T10:00:00Z"
                }
                """.formatted(correlationId);

		rabbitTemplate.convertAndSend(LOG_EXCHANGE, ROUTING_KEY, payload);

		// message delivery is asynchronous — poll until the listener has processed it
		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			List<LogEvent> events = logEventRepository.findAll().stream()
					.filter(e -> correlationId.equals(e.getCorrelationId()))
					.toList();
			assertThat(events).hasSize(1);
			assertThat(events.get(0).getSource()).isEqualTo("HTTP");
			assertThat(events.get(0).getEventType()).isEqualTo("USER_REGISTERED");
		});
	}

	@Test
	void shouldPersistLogEventWithSystemSourceAndGeneratedIdWhenCorrelationIdIsAbsent() {
		// no correlationId field at all — simulates an internal system event,
		// not one propagated from an HTTP request via the gateway
		String uniqueMarker = "system-event-" + UUID.randomUUID();
		String payload = """
                {
                    "serviceName": "test-service",
                    "eventType": "%s",
                    "level": "WARN",
                    "message": "Internal system event with no correlation ID"
                }
                """.formatted(uniqueMarker);

		rabbitTemplate.convertAndSend(LOG_EXCHANGE, ROUTING_KEY, payload);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			List<LogEvent> events = logEventRepository.findAll().stream()
					.filter(e -> uniqueMarker.equals(e.getEventType()))
					.toList();
			assertThat(events).hasSize(1);
			assertThat(events.get(0).getSource()).isEqualTo("SYSTEM");
			assertThat(events.get(0).getCorrelationId()).isNotBlank(); // fallback UUID was generated
		});
	}

	@Test
	void shouldContinueProcessingAfterAMalformedMessage() {
		// a malformed message must not block the queue — subsequent valid messages
		// must still be processed, per LogEventListener's try/catch design
		String uniqueMarker = "after-malformed-" + UUID.randomUUID();
		String malformedPayload = "{ this is not valid JSON at all";
		String validPayload = """
                {
                    "serviceName": "test-service",
                    "eventType": "%s",
                    "level": "INFO",
                    "message": "This one is valid"
                }
                """.formatted(uniqueMarker);

		rabbitTemplate.convertAndSend(LOG_EXCHANGE, ROUTING_KEY, malformedPayload);
		rabbitTemplate.convertAndSend(LOG_EXCHANGE, ROUTING_KEY, validPayload);

		await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
			List<LogEvent> events = logEventRepository.findAll().stream()
					.filter(e -> uniqueMarker.equals(e.getEventType()))
					.toList();
			assertThat(events).hasSize(1);
		});
	}
}