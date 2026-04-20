package leonil.sulude.auth.controller;

import leonil.sulude.auth.exception.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import static org.mockito.Mockito.reset;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.auth.dto.AuthRequest;
import leonil.sulude.auth.dto.AuthResponse;
import leonil.sulude.auth.dto.RegisterRequest;
import leonil.sulude.auth.exception.EmailAlreadyExistsException;
import leonil.sulude.auth.model.Role;
import leonil.sulude.auth.security.JwtAuthenticationFilter;
import leonil.sulude.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller layer test for AuthController.
 *
 * IMPORTANT:
 * ----------
 * This test intentionally isolates the HTTP layer.
 * It does NOT test:
 * - Security
 * - Database
 * - JWT validation
 *
 * It only verifies:
 * - Request mapping
 * - JSON serialization/deserialization
 * - HTTP status codes
 * - Response structure
 */
@WebMvcTest(
        controllers = AuthController.class,

        /*
         * WHY exclude JwtAuthenticationFilter?
         *
         * JwtAuthenticationFilter is annotated with @Component,
         * so Spring automatically tries to register it in the test context.
         *
         * That filter depends on JwtService and UserRepository,
         * which are NOT loaded in @WebMvcTest.
         *
         * If we do not exclude it, Spring will fail to start the context
         * because those dependencies are missing.
         *
         * Therefore, we explicitly tell Spring:
         * "Do NOT register this filter in this test."
         */
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService; // Injected from TestConfig as a mock

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Resets the mocked AuthService before each test execution.
     *
     * WHY:
     * Mockito mocks retain behavior (thenReturn / thenThrow) between tests
     * because the Spring test context is reused.
     *
     * Resetting ensures test isolation and prevents cross-test interference.
     */
    @BeforeEach
    void resetMocks() {
        reset(authService);
    }

    /**
     * Tests successful user registration.
     *
     * This verifies:
     * - The endpoint mapping (/api/auth/register)
     * - Correct HTTP status (200 OK)
     * - Proper JSON response structure
     *
     * The service layer is mocked to isolate controller behavior.
     */
    @Test
    void shouldRegisterSuccessfully() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Leonil",
                "leonil@test.com",
                "password123",
                Role.CLIENT
        );

        AuthResponse response = new AuthResponse(
                "fake-jwt-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "Bearer"
        );

        // Mock service behavior
        Mockito.when(authService.register(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake-jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    /**
     * Tests successful user authentication.
     *
     * This verifies:
     * - The endpoint mapping (/api/auth/login)
     * - Correct HTTP status (200 OK)
     * - Proper token returned in JSON response
     *
     * Business logic is mocked to focus strictly on HTTP layer behavior.
     */
    @Test
    void shouldLoginSuccessfully() throws Exception {

        AuthRequest request = new AuthRequest(
                "leonil@test.com",
                "password123"
        );

        AuthResponse response = new AuthResponse(
                "login-jwt-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                "Bearer"
        );

        Mockito.when(authService.authenticate(Mockito.any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("login-jwt-token"));
    }

    /**
     * Tests registration failure when email already exists.
     *
     * Expected behavior:
     * - Service throws EmailAlreadyExistsException
     * - GlobalExceptionHandler maps it to HTTP 409 (CONFLICT)
     *
     * Ensures proper error propagation from service to HTTP layer.
     */
    @Test
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {

        RegisterRequest request = new RegisterRequest(
                "Leonil",
                "leonil@test.com",
                "password123",
                Role.CLIENT
        );

        Mockito.when(authService.register(Mockito.any()))
                .thenThrow(new EmailAlreadyExistsException("Email already registered."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    /**
     * Tests validation failure for invalid request payload.
     *
     * Expected behavior:
     * - Invalid fields trigger Jakarta Bean Validation
     * - Controller returns HTTP 400 (BAD REQUEST)
     *
     * This verifies that @Valid is properly enforced at controller level.
     */
    @Test
    void shouldReturn400WhenValidationFails() throws Exception {

        RegisterRequest invalidRequest = new RegisterRequest(
                "",                     // invalid name
                "invalid-email",         // invalid email format
                "",                      // invalid password
                Role.CLIENT
        );

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Tests authentication failure due to invalid credentials.
     *
     * Expected behavior:
     * - Service throws authentication-related exception
     * - GlobalExceptionHandler maps it to HTTP 401 (UNAUTHORIZED)
     *
     * Ensures login errors are correctly translated into HTTP responses.
     */
    @Test
    void shouldReturn401WhenCredentialsInvalid() throws Exception {

        AuthRequest request = new AuthRequest(
                "wrong@test.com",
                "wrong-password"
        );

        Mockito.when(authService.authenticate(Mockito.any()))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    /**
     * Test configuration used only for this test class.
     *
     * WHY THIS EXISTS:
     * ----------------
     * @WebMvcTest loads only the web layer.
     * It does NOT load service beans automatically.
     *
     * Therefore, we provide:
     * 1. A mocked AuthService
     * 2. A permissive SecurityFilterChain
     */
    @TestConfiguration
    static class TestConfig {

        /**
         * Provide a Mockito mock instead of the real AuthService.
         *
         * This keeps the test focused on controller behavior.
         */
        @Bean
        AuthService authService() {
            return Mockito.mock(AuthService.class);
        }

        /**
         * Disable security constraints for this test.
         *
         * Without this, POST requests would fail due to CSRF protection
         * or authentication requirements.
         *
         * This does NOT affect production configuration.
         */
        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

            return http.build();
        }
    }
}