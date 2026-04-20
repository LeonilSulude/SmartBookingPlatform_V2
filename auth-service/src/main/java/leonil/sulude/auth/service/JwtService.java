package leonil.sulude.auth.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import leonil.sulude.auth.security.JwtProperties;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Getter
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties){

        this.jwtProperties = jwtProperties;
    }

    /**
     * Retrieves the signing key used to sign and validate JWT tokens.
     * Uses HMAC SHA256 algorithm with the provided secret.
     */
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    /**
     * Generates a signed JWT token containing user ID, email, and role.
     * The token has an expiration date based on configuration.
     *
     * @param userId the unique identifier of the user
     * @param email user's email
     * @param role user's role
     * @return a JWT token string
     */
    public String generateToken(UUID userId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.getExpiration());

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates the integrity and expiration of a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if valid, false otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Extracts claims (payload) from a valid JWT token.
     *
     * @param token the JWT token
     * @return claims object containing token data
     */
    public Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }


}
