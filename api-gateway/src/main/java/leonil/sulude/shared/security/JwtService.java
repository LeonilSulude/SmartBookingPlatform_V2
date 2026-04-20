package leonil.sulude.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.function.Function;

@Service
public class JwtService {

    // Injects the JWT secret from environment variables or application.yml
    @Value("${security.jwt.secret}")
    private String secret;

    /**
     * Validates the token's signature and expiration.
     *
     * @param token the JWT to validate
     * @return true if the token is valid
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts the username (email) from the token.
     *
     * @param token the JWT
     * @return the subject (usually email or user ID)
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Generic method to extract a specific claim.
     *
     * @param token the JWT
     * @param claimsResolver function to extract a claim
     * @return the desired claim
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and verifies the JWT using the secret key.
     *
     * @param token the JWT
     * @return all claims
     */
    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Builds the signing key from the configured secret.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}
