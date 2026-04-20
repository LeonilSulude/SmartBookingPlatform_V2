package leonil.sulude.auth.service;

import leonil.sulude.auth.dto.AuthRequest;
import leonil.sulude.auth.dto.AuthResponse;
import leonil.sulude.auth.dto.RegisterRequest;
import leonil.sulude.auth.exception.EmailAlreadyExistsException;
import leonil.sulude.auth.exception.InvalidCredentialsException;
import leonil.sulude.auth.model.User;
import leonil.sulude.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * AuthServiceImpl provides the concrete implementation for user registration
 * and authentication using JWT tokens.
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new user if the email doesn't already exist.
     * Encodes the password and stores the user in the database,
     * then generates a JWT token.
     */
    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already registered.");
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtService.getJwtProperties().getExpiration());

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthResponse(
                token,
                issuedAt,
                expiresAt,
                "Bearer"
        );

    }

    /**
     * Authenticates a user by validating their credentials.
     * If valid, returns a JWT token. Otherwise, throws an error.
     */
    @Override
    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtService.getJwtProperties().getExpiration());

        return new AuthResponse(
                token,
                issuedAt,
                expiresAt,
                "Bearer"
        );
    }
}
