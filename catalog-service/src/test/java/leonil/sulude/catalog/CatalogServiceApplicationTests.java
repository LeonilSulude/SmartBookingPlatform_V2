package leonil.sulude.catalog;

import leonil.sulude.catalog.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Closes the V1-era "infrastructure tests require Docker" limitation the same way
 * LogEventProducerTest was fixed — real Postgres and Kafka via Testcontainers,
 * no manual docker-compose dependency and no @ActiveProfiles Kafka exclusion needed.
 */
class CatalogServiceApplicationTests extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}