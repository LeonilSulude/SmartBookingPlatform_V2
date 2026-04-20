package leonil.sulude.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import leonil.sulude.auth.dto.AuthRequest;
import leonil.sulude.auth.dto.AuthResponse;
import leonil.sulude.auth.dto.RegisterRequest;
import leonil.sulude.auth.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(
        name = "Authentication",
        description = "Endpoints for user registration and authentication"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Endpoint to register a new user.
     * Accepts a RegisterRequest (email, password, role).
     * Returns a JWT token upon successful registration.
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account and returns a JWT token upon successful registration."
    )
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "409", description = "Email already registered")
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to authenticate a user.
     * Accepts an AuthRequest (email and password).
     * Returns a JWT token if the credentials are valid.
     */
    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user using email and password and returns a JWT token if the credentials are valid."
    )
    @ApiResponse(responseCode = "200", description = "Authentication successful")
    @ApiResponse(responseCode = "401", description = "Invalid credentials")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }
}