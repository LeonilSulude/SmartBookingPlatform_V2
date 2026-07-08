package leonil.sulude.booking.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;

/**
 * Base class for integration tests requiring real PostgreSQL and Kafka.
 * Also starts a WireMock server to simulate the Catalog Service --- the Booking Service
 * is tested in isolation, without requiring the real Catalog Service to be running.
 *
 * WireMock is started in a static block, not @BeforeEach --- ApplicationReadyEvent
 * (which triggers ResourceCacheSeeder) fires while the Spring context is loading,
 * which happens before any @BeforeEach method runs. Starting WireMock statically
 * guarantees it is already listening before the context --- and the seeder --- starts.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16"))
            .withDatabaseName("booking_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
                    .asCompatibleSubstituteFor("confluentinc/cp-kafka"));

    protected static final WireMockServer wireMockServer = new WireMockServer(8089);

    static {
        // started before the Spring context loads — the seeder needs this alive on ApplicationReadyEvent
        wireMockServer.start();
        configureFor("localhost", 8089);

        // stub for the seeder's own startup call — returns an empty list by default,
        // individual tests override this stub if they need specific seeded data
        com.github.tomakehurst.wiremock.client.WireMock.stubFor(
                com.github.tomakehurst.wiremock.client.WireMock.get("/api/resources")
                        .willReturn(com.github.tomakehurst.wiremock.client.WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("[]")));
    }

    @org.junit.jupiter.api.BeforeEach
    void resetWireMockRequestLog() {
        // clears the request history so verify() counts are accurate per test —
        // stub mappings themselves (what to respond with) are untouched, so the
        // seeder's /api/resources stub keeps working across all tests in this class
        resetAllRequests();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);

        // override the Feign client URL — bypasses Eureka discovery, points directly to WireMock
        registry.add("spring.cloud.openfeign.client.config.catalog-service.url",
                () -> "http://localhost:8089");
    }
}