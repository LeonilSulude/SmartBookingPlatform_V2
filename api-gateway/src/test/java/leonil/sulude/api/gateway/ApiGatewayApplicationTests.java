package leonil.sulude.api.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies the application context loads successfully.
 * Needs security.jwt.secret because JwtService requires it with no default value —
 * without it, context loading fails before any test logic even runs. eureka.client.enabled
 * is disabled for the same isolation reason used in the other Gateway IT classes: prevents
 * this test from registering with a real, locally-running Eureka server if one happens
 * to be up, which would make this simple smoke test depend on the state of the machine.
 */
@SpringBootTest(properties = {
		"security.jwt.secret=test-secret-test-secret-test-secret-32b",
		"eureka.client.enabled=false"
})
class ApiGatewayApplicationTests {
	@Test
	void contextLoads() {
	}
}