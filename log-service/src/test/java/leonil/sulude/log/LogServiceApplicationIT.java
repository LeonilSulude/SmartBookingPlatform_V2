package leonil.sulude.log;

import leonil.sulude.log.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Closes the V1-era "infrastructure tests require Docker" limitation — this test
 * now brings its own Postgres and RabbitMQ automatically via Testcontainers.
 *
 * IT suffix (not Tests) --- needs real Testcontainers infrastructure via
 * AbstractIntegrationTest, so it must run under Failsafe (mvn verify), not Surefire
 * (mvn test): Surefire's default include patterns also match "*Tests.java", which is
 * how this ended up needing Docker during what was meant to be the fast unit-test phase.
 */
class LogServiceApplicationIT extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
    }
}
