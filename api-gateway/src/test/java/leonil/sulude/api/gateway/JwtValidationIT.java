package leonil.sulude.api.gateway;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.security.Key;
import java.util.Date;

/**
 * Tests JWT validity handling at the API Gateway — expired tokens, tampered signatures,
 * missing role claims, and the read-access path for any authenticated role. Complements
 * RoleBasedAuthorizationIT, which covers role-based authorization decisions specifically.
 *
 * Same 503-vs-401-vs-403 pattern as RoleBasedAuthorizationIT: no downstream services run
 * during this test, so a request that passes both authentication and authorization reaches
 * routing and gets 503 (no instance found) rather than a client error — proving the security
 * decision itself was correct without needing a real or stubbed downstream service.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "security.jwt.secret=test-secret-test-secret-test-secret-32b",
                "eureka.client.enabled=false"
        }
)
@AutoConfigureWebTestClient
class JwtValidationIT {

    private static final String SECRET = "test-secret-test-secret-test-secret-32b";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void shouldRejectExpiredToken() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String expiredToken = Jwts.builder()
                .setSubject("client@test.com")
                .claim("role", "CLIENT")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7_200_000)) // 2h ago
                .setExpiration(new Date(System.currentTimeMillis() - 3_600_000)) // expired 1h ago
                .signWith(key)
                .compact();

        webTestClient.get()
                .uri("/api/bookings")
                .header("Authorization", "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldRejectTokenWithTamperedSignature() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String validToken = Jwts.builder()
                .setSubject("client@test.com")
                .claim("role", "CLIENT")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();

        // flip a character in the signature segment — token structurally looks valid
        // (still three base64 segments) but the signature no longer matches the payload
        String tamperedToken = validToken.substring(0, validToken.length() - 1)
                + (validToken.endsWith("A") ? "B" : "A");

        webTestClient.get()
                .uri("/api/bookings")
                .header("Authorization", "Bearer " + tamperedToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void shouldAllowAnyAuthenticatedRoleToReadCatalog() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        String token = Jwts.builder()
                .setSubject("client@test.com")
                .claim("role", "CLIENT")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();

        // GET routes only require .authenticated() — any valid role should pass,
        // reaching routing (503, no downstream) rather than being rejected (403)
        webTestClient.get()
                .uri("/api/offers")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isEqualTo(503);
    }

    @Test
    void shouldRejectValidTokenWithoutRoleClaim() {
        Key key = Keys.hmacShaKeyFor(SECRET.getBytes());
        // structurally valid, correctly signed token — but no "role" claim at all,
        // simulating a token issued by a misconfigured or older Auth Service version
        String tokenWithoutRole = Jwts.builder()
                .setSubject("nobody@test.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();

        // the filter sets an empty authorities list when role is null — must not be
        // silently treated as having access to a role-specific route
        webTestClient.post()
                .uri("/api/bookings")
                .header("Authorization", "Bearer " + tokenWithoutRole)
                .exchange()
                .expectStatus().isForbidden();
    }
}