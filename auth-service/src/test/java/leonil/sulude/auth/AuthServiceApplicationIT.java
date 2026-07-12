package leonil.sulude.auth;

import leonil.sulude.auth.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Verifies the application context loads successfully with real infrastructure.
 * Unlike its Catalog/Log/Gateway/Booking siblings, this smoke test had never been
 * migrated off the V1-era plain @SpringBootTest pattern, so it silently depended on
 * a manually-running Postgres on localhost:5432 — masked in earlier sessions simply
 * because that instance happened to be running at the time, and only surfaced when
 * run in isolation (e.g. in CI, with no such instance available).
 *
 * IT suffix (not Tests) --- needs real Testcontainers Postgres via AbstractIntegrationTest,
 * so it must run under Failsafe (mvn verify), not Surefire (mvn test): Surefire's default
 * include patterns also match "*Tests.java", which is how this ended up needing Docker
 * during what was meant to be the fast unit-test phase.
 */
class AuthServiceApplicationIT extends AbstractIntegrationTest {

	@Test
	void contextLoads() {
	}
}
