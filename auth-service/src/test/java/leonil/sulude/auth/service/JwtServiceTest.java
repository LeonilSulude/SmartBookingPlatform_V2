package leonil.sulude.auth.service;

import io.jsonwebtoken.Claims;
import leonil.sulude.auth.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 *
 * PURPOSE:
 * --------
 * These tests validate JWT generation, validation and claim extraction
 * without starting the Spring context.
 *
 * WHY UNIT TEST (no @SpringBootTest)?
 * -----------------------------------
 * JwtService contains pure business/security logic.
 * It does not require database, web layer or Spring container.
 * Keeping this as a pure unit test makes it:
 * - Faster
 * - Isolated
 * - Easier to debug
 */
class JwtServiceTest {

    private JwtService jwtService;

    // Strong secret (>= 256 bits recommended for HS256)
    private static final String SECRET =
            "q9RkT9mXc4Wv7Zp2Hs6Df1Bj3Ly9Na5Gu0Pe2Vr9KwXt6YzQ";

    @BeforeEach
    void setup() {

        /*
         * We manually construct JwtProperties instead of using Spring.
         * This keeps the test independent from the application context.
         */
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);

        // 1 hour expiration
        props.setExpiration(1000 * 60 * 60);

        jwtService = new JwtService(props);
    }

    @Test
    void shouldGenerateValidToken() {

        UUID userId = UUID.randomUUID();
        String email = "test@email.com";
        String role = "USER";

        // Generate token
        String token = jwtService.generateToken(userId, email, role);

        // Basic assertions
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token),
                "Generated token should be valid");
    }

    @Test
    void shouldExtractClaimsCorrectly() {

        UUID userId = UUID.randomUUID();
        String email = "test@email.com";
        String role = "ADMIN";

        String token = jwtService.generateToken(userId, email, role);

        /*
         * extractClaims internally:
         * - Validates signature
         * - Validates expiration
         * - Returns token payload (claims)
         */
        Claims claims = jwtService.extractClaims(token);

        assertEquals(userId.toString(), claims.getSubject());
        assertEquals(email, claims.get("email"));
        assertEquals(role, claims.get("role"));
    }

    @Test
    void shouldReturnFalseForInvalidToken() {

        String fakeToken = "invalid.token.value";

        /*
         * isTokenValid should catch parsing exceptions
         * and return false instead of throwing.
         */
        assertFalse(jwtService.isTokenValid(fakeToken),
                "Invalid token should return false");
    }

    @Test
    void shouldValidateOnlyWithCorrectSecret() {

        UUID userId = UUID.randomUUID();

        // Service A (correct secret)
        JwtProperties propsA = new JwtProperties();
        propsA.setSecret(SECRET);
        propsA.setExpiration(1000 * 60 * 60);
        JwtService jwtServiceA = new JwtService(propsA);

        String token = jwtServiceA.generateToken(
                userId,
                "test@email.com",
                "USER"
        );

        // First, validate with correct secret
        assertTrue(jwtServiceA.isTokenValid(token),
                "Token should be valid with correct secret");

        // Service B (wrong secret)
        JwtProperties propsB = new JwtProperties();
        propsB.setSecret("DifferentSecretThatIsAlsoLongEnough123456");
        propsB.setExpiration(1000 * 60 * 60);
        JwtService jwtServiceB = new JwtService(propsB);

        // Then validate with wrong secret
        assertFalse(jwtServiceB.isTokenValid(token),
                "Token should be invalid with different secret");
    }

    @Test
    void shouldReturnFalseForExpiredToken() throws InterruptedException {

        // Expiration set to 100 milliseconds
        JwtProperties shortLivedProps = new JwtProperties();
        shortLivedProps.setSecret(SECRET);
        shortLivedProps.setExpiration(100);

        JwtService shortLivedJwtService = new JwtService(shortLivedProps);

        UUID userId = UUID.randomUUID();

        String token = shortLivedJwtService.generateToken(
                userId,
                "expired@email.com",
                "USER"
        );

        // Wait for token to expire
        Thread.sleep(200);

        assertFalse(shortLivedJwtService.isTokenValid(token),
                "Expired token should be invalid");
    }



    @Test
    void shouldReturnFalseForTamperedToken() {

        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(
                userId,
                "test@email.com",
                "USER"
        );

        // Flip a character roughly in the middle of the token rather than the last one.
        // The old version changed only the final character, which for a base64-encoded
        // 256-bit signature (6 bits/char) can land on a "don't care" padding boundary with
        // no real information in it — sometimes leaving the underlying signature bytes
        // unchanged, so the "tampered" token was still valid. Since the token is timestamped
        // fresh every run, whether that happened varied from run to run, making the test
        // flaky rather than reliably red. The middle of the token falls within the payload
        // segment, so changing it always invalidates the signature deterministically.
        int tamperIndex = token.length() / 2;
        char originalChar = token.charAt(tamperIndex);
        char replacementChar = originalChar == 'A' ? 'B' : 'A';
        String tamperedToken = token.substring(0, tamperIndex)
                + replacementChar
                + token.substring(tamperIndex + 1);

        assertFalse(jwtService.isTokenValid(tamperedToken),
                "Modified token should be invalid");
    }
}