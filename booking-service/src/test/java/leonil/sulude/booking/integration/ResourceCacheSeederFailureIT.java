package leonil.sulude.booking.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for ResourceCacheSeeder's failure path --- the Catalog Service is
 * completely unreachable at startup, forcing all 3 retry attempts to fail.
 *
 * This class intentionally does NOT extend AbstractIntegrationTest. That base class's
 * static WireMock stub always answers GET /api/resources with 200 and an empty list,
 * which would defeat the exact scenario this test needs: every seeder attempt failing.
 * A separate context also avoids polluting the shared WireMock/circuit breaker state
 * used by every other Booking Service IT class.
 *
 * Postgres and Kafka containers are started fresh here rather than reused from the
 * shared singleton pattern --- this class runs once, in isolation, so the extra startup
 * cost is acceptable in exchange for a fully independent, unpolluted Spring context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ResourceCacheSeederFailureIT {

    static PostgreSQLContainer<?> postgres;
    static KafkaContainer kafka;
    static WireMockServer wireMockServer;

    @Autowired
    private ResourceCacheRepository resourceCacheRepository;

    @BeforeAll
    static void startInfra() {
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"))
                .withDatabaseName("booking_seeder_test")
                .withUsername("test_user")
                .withPassword("test_pass");
        postgres.start();

        kafka = new KafkaContainer(
                DockerImageName.parse("apache/kafka:3.7.0")
                        .asCompatibleSubstituteFor("confluentinc/cp-kafka"));
        kafka.start();

        // a different port from the shared 8089 used elsewhere — avoids any chance
        // of collision if tests were ever run in parallel
        wireMockServer = new WireMockServer(8199);
        wireMockServer.start();
        configureFor("localhost", 8199);

        // every call to the seeder's startup endpoint fails — simulates Catalog being
        // completely unreachable, forcing all 3 retry attempts to exhaust
        stubFor(get("/api/resources")
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));
    }

    @AfterAll
    static void stopInfra() {
        wireMockServer.stop();
        kafka.stop();
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.kafka.bootstrap-servers", () -> kafka.getBootstrapServers());
        registry.add("spring.cloud.openfeign.client.config.catalog-service.url",
                () -> "http://localhost:8199");
    }

    @Test
    void shouldStartWithEmptyCacheWhenCatalogIsUnreachableAtStartup() {
        // the application context loaded successfully despite every seed attempt failing —
        // this is the core assertion: a Catalog outage at startup must not prevent the
        // Booking Service itself from starting. @Recover in ResourceCacheSeeder caught
        // the exhausted retries and logged a warning instead of propagating the failure
        assertThat(resourceCacheRepository.findAll()).isEmpty();

        // the seeder's own stub was hit multiple times — proof Spring Retry's 3 attempts
        // (1s, 2s, 4s backoff) actually executed, not just a single failed call
        verify(moreThan(1), getRequestedFor(urlEqualTo("/api/resources")));
    }
}