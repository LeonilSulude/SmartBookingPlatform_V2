package leonil.sulude.catalog;

import leonil.sulude.catalog.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Closes the V1-era "infrastructure tests require Docker" limitation the same way
 * LogEventProducerIT was fixed — real Postgres and Kafka via Testcontainers,
 * no manual docker-compose dependency and no @ActiveProfiles Kafka exclusion needed.
 *
 * IT suffix (not Tests) --- needs real Testcontainers Postgres/Kafka via
 * AbstractIntegrationTest, so it must run under Failsafe (mvn verify), not Surefire
 * (mvn test): Surefire's default include patterns also match "*Tests.java", which is
 * how this ended up needing Docker during what was meant to be the fast unit-test phase.
 */
class CatalogServiceApplicationIT extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}
