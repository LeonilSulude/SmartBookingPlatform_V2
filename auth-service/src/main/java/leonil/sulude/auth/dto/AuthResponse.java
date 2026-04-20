package leonil.sulude.auth.dto;

import java.time.Instant;

public record AuthResponse(
        String token,
        Instant issuedAt,
        Instant expiresAt,
        String tokenType // ex: "Bearer"
) {}
