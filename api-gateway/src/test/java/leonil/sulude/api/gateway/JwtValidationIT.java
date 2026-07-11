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

        // flip a character roughly in the middle of the token — for a typical JWT
        // (header.payload.signature) this index usually lands within the payload segment,
        // since it's normally the longest part. Changing the payload guarantees the
        // signature (computed over the ORIGINAL header+payload) no longer matches,
        // deterministically — no dependency on base64 padding luck.
        // Contrast with the previous version of this test, which flipped only the LAST
        // character of the whole token (landing in the signature segment): a JWT signature
        // is 256 bits, base64-encoded 6 bits per character, so its final character can fall
        // on a "don't care" padding boundary with no real information in it. Flipping that
        // exact character sometimes left the underlying signature bytes unchanged, making
        // the "tampered" token still valid — and since the JWT is timestamped fresh on every
        // run, whether the last character happened to be meaningful or padding varied from
        // run to run, making the old test flaky rather than reliably red.
        int tamperIndex = validToken.length() / 2;
        char originalChar = validToken.charAt(tamperIndex);
        char replacementChar = originalChar == 'A' ? 'B' : 'A';
        String tamperedToken = validToken.substring(0, tamperIndex)
                + replacementChar
                + validToken.substring(tamperIndex + 1);

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