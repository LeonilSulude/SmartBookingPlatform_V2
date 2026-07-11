package leonil.sulude.log;

import leonil.sulude.log.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Closes the V1-era "infrastructure tests require Docker" limitation — this test
 * now brings its own Postgres and RabbitMQ automatically via Testcontainers.
 */
class LogServiceApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}