package leonil.sulude.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Main security configuration bean for Spring WebFlux.
     *
     * Defines:
     * - What paths are public
     * - What filter handles authentication (JWT filter)
     * - Stateless security behavior (no sessions)
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // Disable CSRF since this is an API Gateway and uses tokens
                .csrf(ServerHttpSecurity.CsrfSpec::disable)

                // Disable default login page or basic auth
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                // Define route rules
                .authorizeExchange(exchanges -> exchanges
                        // Allow open access to the auth service
                        .pathMatchers("/api/auth/**").permitAll()

                        //TESTING
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/test").permitAll()


                        // All other routes require authentication
                        .anyExchange().authenticated()
                )

                // Apply custom JWT authentication filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}
