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
     * IMPORTANT — rule ordering: authorizeExchange() evaluates rules in the order they
     * are declared and stops at the FIRST match, like a chain of if/else, not "most
     * specific wins". A previous version placed pathMatchers("/**").hasAuthority("ADMIN")
     * BEFORE the PROVIDER/CLIENT-specific rules — since "/**" matches every path, it
     * silently intercepted every request before the more specific rules could ever be
     * reached, meaning only ADMIN could access anything beyond the public routes. This
     * was discovered via RoleBasedAuthorizationIT and confirmed manually via Postman
     * against the gateway (not a direct call to a downstream service, which bypasses
     * this filter chain entirely). Specific rules must always be declared before
     * general/catch-all ones.
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)

                .authorizeExchange(exchanges -> exchanges
                        // public endpoints — no token required
                        .pathMatchers("/api/auth/**").permitAll()
                        .pathMatchers("/actuator/**").permitAll()
                        .pathMatchers("/test").permitAll()

                        // ADMIN can also perform these actions — declared alongside the
                        // role that primarily owns each action, via hasAnyAuthority(),
                        // rather than as a separate catch-all rule that would shadow
                        // everything declared after it
                        .pathMatchers(HttpMethod.POST, "/api/offers/**", "/api/resources/**")
                        .hasAnyAuthority("PROVIDER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/offers/**", "/api/resources/**")
                        .hasAnyAuthority("PROVIDER", "ADMIN")

                        .pathMatchers(HttpMethod.POST, "/api/bookings/**")
                        .hasAnyAuthority("CLIENT", "ADMIN")

                        // authenticated users (any role) can read catalog and bookings
                        .pathMatchers(HttpMethod.GET, "/api/offers/**", "/api/resources/**", "/api/bookings/**")
                        .authenticated()

                        // safety net — anything not explicitly matched above still
                        // requires at least a valid, authenticated token
                        .anyExchange().authenticated()
                )

                .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }
}