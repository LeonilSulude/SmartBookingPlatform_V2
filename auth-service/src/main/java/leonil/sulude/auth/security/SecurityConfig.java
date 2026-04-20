package leonil.sulude.auth.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter; // Custom filter that validates JWT on each request

    /**
     * Defines the security filter chain and how requests are handled.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable()) // Disable CSRF for stateless APIs
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // No HTTP session
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Allow registration & login
                        .requestMatchers("/test-secret").permitAll() // Allow registration & login
                        .requestMatchers("/actuator/**").permitAll() // Allow monitoring endpoints
                        .requestMatchers("/swagger-ui/**",
                                                  "/v3/api-docs/**").permitAll() // Allow Swagger UI and OpenAPI docs
                        .anyRequest().authenticated() // All other requests require authentication
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // Register our custom JWT filter
                .build();
    }

    /**
     * Exposes the AuthenticationManager used for programmatic login (e.g., in AuthService).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
