package leonil.sulude.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.http.HttpMethod;

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
                        // public endpoints — no token required
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/test").permitAll()

                        // ADMIN has full access to all routes
                        .pathMatchers("/**").hasAuthority("ADMIN")

                        // only PROVIDER can manage offers and resources
                        .pathMatchers(HttpMethod.POST, "/api/offers/**", "/api/resources/**").hasAuthority("PROVIDER")
                        .pathMatchers(HttpMethod.DELETE, "/api/offers/**", "/api/resources/**").hasAuthority("PROVIDER")

                        // only CLIENT can create bookings
                        .pathMatchers(HttpMethod.POST, "/api/bookings/**").hasAuthority("CLIENT")

                        // authenticated users can read catalog and their bookings
                        .pathMatchers(HttpMethod.GET, "/api/offers/**", "/api/resources/**", "/api/bookings/**").authenticated()

                        // everything else requires authentication
                        .anyExchange().authenticated()
                )

                // Apply custom JWT authentication filter
                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}
