package leonil.sulude.api.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests validating API Gateway security and routing behavior.
 *
 * These tests confirm that:
 * - Public endpoints remain accessible without authentication
 * - Protected routes correctly require JWT authentication
 * - Gateway routing is correctly configured
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "security.jwt.secret=test-secret"
)
@AutoConfigureWebTestClient
class GatewayRoutesTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Auth endpoints are public and bypass authentication filters.
     *
     * The request passes the security layer and the gateway successfully
     * matches the configured route (/api/auth/**). However, because the
     * auth-service is not running during this test, the gateway cannot
     * resolve the target service via service discovery and returns
     * 503 Service Unavailable instead of a client error.
     */
    @Test
    void shouldAllowAuthEndpointsWithoutToken() {

        webTestClient.get()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isEqualTo(503); //Service Unavailable
    }

    /**
     * Verifies that catalog routes are protected by authentication.
     * Requests without a JWT token should return 401 Unauthorized.
     */
    @Test
    void shouldProtectCatalogRoutes() {

        webTestClient.get()
                .uri("/api/offers")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    /**
     * Verifies that booking routes are protected by authentication.
     * Requests without a JWT token should return 401 Unauthorized.
     */
    @Test
    void shouldProtectBookingRoutes() {

        webTestClient.get()
                .uri("/api/bookings")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}