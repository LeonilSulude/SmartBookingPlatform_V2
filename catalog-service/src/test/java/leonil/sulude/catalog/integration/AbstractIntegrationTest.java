package leonil.sulude.catalog.integration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.StreamSupport;

/**
 * Base class for Catalog Service integration tests requiring real PostgreSQL and Kafka.
 *
 * Uses the singleton container pattern from the start (lesson learned in the Booking
 * Service test suite): containers are started manually in a static block and NEVER
 * stopped explicitly. With @Container-managed lifecycle on a static field inherited by
 * multiple IT classes, JUnit stops the container after each class's tests finish — the
 * first class to complete would kill it for every subsequent class sharing the same
 * inherited field, causing connection-refused failures. Ryuk cleans up all containers
 * automatically when the JVM exits, regardless of how many classes shared them.
 *
 * Unlike the Booking Service, no WireMock is needed here — the Catalog Service does not
 * make outbound synchronous calls to any other service; it only publishes Kafka events.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres;
    protected static final KafkaContainer kafka;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("catalog_test")
                .withUsername("test_user")
                .withPassword("test_pass");
        postgres.start();

        kafka = new KafkaContainer(
                DockerImageName.parse("apache/kafka:3.7.0")
                        .asCompatibleSubstituteFor("confluentinc/cp-kafka"));
        kafka.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    /**
     * Consumes messages from a Kafka topic as raw JSON strings, for tests to verify
     * what the producer actually published --- a plain KafkaConsumer, not a Spring
     * @KafkaListener, since we only need this for assertions, not for wiring into
     * application logic. A fresh consumer group per call avoids offset conflicts
     * between test methods reading the same topic.
     *
     * @param topic   the Kafka topic to read from
     * @param timeout how long to wait for messages before giving up
     * @return the raw JSON string bodies of all messages received within the timeout
     */
    protected List<String> consumeMessages(String topic, Duration timeout) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topic));
            ConsumerRecords<String, String> records = consumer.poll(timeout);
            return StreamSupport.stream(records.spliterator(), false)
                    .map(ConsumerRecord::value)
                    .toList();
        }
    }
}