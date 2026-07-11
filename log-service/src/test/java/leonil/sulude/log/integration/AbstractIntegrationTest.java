package leonil.sulude.log.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for Log Service integration tests requiring real PostgreSQL and RabbitMQ.
 *
 * Singleton container pattern (lesson learned in the Booking Service test suite):
 * containers are started manually in a static block and NEVER stopped explicitly.
 * Ryuk cleans them up automatically when the JVM exits, regardless of how many IT
 * classes shared them.
 *
 * No Kafka here — the Log Service consumes only from RabbitMQ.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    protected static final PostgreSQLContainer<?> postgres;
    protected static final RabbitMQContainer rabbitMQ;

    static {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("log_test")
                .withUsername("test_user")
                .withPassword("test_pass");
        postgres.start();

        rabbitMQ = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3-management"));
        rabbitMQ.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }
}