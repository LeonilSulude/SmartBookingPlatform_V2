package leonil.sulude.booking;

import leonil.sulude.booking.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Closes the same V1-era "infrastructure tests require Docker" limitation already
 * fixed the same way for the Catalog, Log, and Gateway services — this one was
 * simply overlooked until now, since the platform was often running in earlier
 * sessions, masking the fact that it still depended on @ActiveProfiles("test")
 * property exclusions rather than real Testcontainers infrastructure.
 *
 * IT suffix (not Tests) --- needs real Testcontainers Postgres/Kafka via
 * AbstractIntegrationTest, so it must run under Failsafe (mvn verify), not Surefire
 * (mvn test): Surefire's default include patterns also match "*Tests.java", which is
 * how this ended up needing Docker during what was meant to be the fast unit-test phase.
 */
class BookingServiceApplicationIT extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}
