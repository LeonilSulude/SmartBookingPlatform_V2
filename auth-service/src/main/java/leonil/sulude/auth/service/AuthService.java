package leonil.sulude.auth.service;

import leonil.sulude.auth.dto.AuthRequest;
import leonil.sulude.auth.dto.AuthResponse;
import leonil.sulude.auth.dto.RegisterRequest;

/**
 * AuthService defines the contract for authentication-related operations.
 * It is implemented by AuthServiceImpl and can be mocked or extended easily.
 */
public interface AuthService {

    /**
     * Registers a new user and generates a JWT token upon successful registration.
     *
     * @param request DTO containing user email, password, and role.
     * @return AuthResponse with generated token.
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates an existing user and generates a JWT token.
     *
     * @param request DTO with user email and password.
     * @return AuthResponse with generated token if credentials are valid.
     */
    AuthResponse authenticate(AuthRequest request);
}
