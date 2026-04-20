package leonil.sulude.auth.service;

import leonil.sulude.auth.dto.AuthRequest;
import leonil.sulude.auth.dto.RegisterRequest;
import leonil.sulude.auth.exception.EmailAlreadyExistsException;
import leonil.sulude.auth.exception.InvalidCredentialsException;
import leonil.sulude.auth.model.Role;
import leonil.sulude.auth.model.User;
import leonil.sulude.auth.repository.UserRepository;
import leonil.sulude.auth.security.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl.
 *
 * These tests validate business logic in isolation.
 * No Spring context, no real database, no real JWT signing.
 *
 * We mock:
 * - UserRepository → avoids database access
 * - PasswordEncoder → avoids real hashing
 * - JwtService → avoids real token generation
 *
 * Goal:
 * Ensure that service logic behaves correctly under all scenarios.
 */
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // We mock expiration because AuthService uses it to calculate expiry timestamp
        JwtProperties props = new JwtProperties();
        props.setExpiration(3600000);

        when(jwtService.getJwtProperties()).thenReturn(props);
    }

    // ============================================================
    // REGISTER TESTS
    // ============================================================

    @Test
    void shouldRegisterUserSuccessfully() {

        /*
         * Scenario:
         * - Email does not exist
         * - Password gets encoded
         * - User is saved
         * - JWT is generated
         */

        RegisterRequest request =
                new RegisterRequest("John", "john@email.com", "password", Role.CLIENT);

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(), any(), any())).thenReturn("fakeToken");

        // Simulate saved user returned from repository
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .name("John")
                .email("john@email.com")
                .password("encodedPassword")
                .role(Role.CLIENT)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        assertNotNull(response);
        assertEquals("fakeToken", response.token());

        // Verify critical interactions
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password");
        verify(jwtService).generateToken(any(), any(), any());
    }

    @Test
    void shouldThrowIfEmailAlreadyExists() {

        /*
         * Scenario:
         * - Email already exists
         * - Registration must fail
         * - No user should be saved
         */

        RegisterRequest request =
                new RegisterRequest("John", "john@email.com", "password", Role.CLIENT);

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> authService.register(request));

        // Ensure no persistence happens
        verify(userRepository, never()).save(any());
    }

    // ============================================================
    // AUTHENTICATE TESTS
    // ============================================================

    @Test
    void shouldAuthenticateSuccessfully() {

        /*
         * Scenario:
         * - User exists
         * - Password matches
         * - JWT token is returned
         */

        UUID userId = UUID.randomUUID();

        User user = User.builder()
                .id(userId)
                .email("john@email.com")
                .password("encodedPassword")
                .role(Role.CLIENT)
                .build();

        AuthRequest request =
                new AuthRequest("john@email.com", "password");

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("password", "encodedPassword"))
                .thenReturn(true);

        when(jwtService.generateToken(any(), any(), any()))
                .thenReturn("fakeToken");

        var response = authService.authenticate(request);

        assertEquals("fakeToken", response.token());

        // Ensure JWT was generated with correct user data
        verify(jwtService).generateToken(userId, user.getEmail(), user.getRole().name());
    }

    @Test
    void shouldThrowIfUserNotFound() {

        /*
         * Scenario:
         * - Email not found in database
         * - Should throw UsernameNotFoundException
         */

        AuthRequest request =
                new AuthRequest("notfound@email.com", "password");

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> authService.authenticate(request));
    }

    @Test
    void shouldThrowIfPasswordInvalid() {

        /*
         * Scenario:
         * - User exists
         * - Password does NOT match
         * - Authentication must fail
         */

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("john@email.com")
                .password("encodedPassword")
                .role(Role.CLIENT)
                .build();

        AuthRequest request =
                new AuthRequest("john@email.com", "wrongPassword");

        when(userRepository.findByEmail(request.email()))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        assertThrows(InvalidCredentialsException.class,
                () -> authService.authenticate(request));
    }
}