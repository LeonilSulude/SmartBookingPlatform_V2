package leonil.sulude.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * JWT Authentication Filter for Spring Cloud Gateway using WebFlux.
 *
 * <p>This filter intercepts every incoming HTTP request to the API Gateway and:
 * <ul>
 *   <li>Extracts the JWT token from the Authorization header</li>
 *   <li>Validates the token using JwtService</li>
 *   <li>If valid, creates an Authentication object with a dummy User</li>
 *   <li>Stores the authentication in the reactive security context</li>
 * </ul>
 *
 * <p>It does NOT call a database to load user details. It assumes that if the token is valid,
 * the request can be forwarded to downstream services with security context pre-populated.</p>
 *
 * <p><strong>Expected Authorization header format:</strong> {@code Authorization: Bearer <token>}</p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    // Service responsible for token parsing and validation
    private final JwtService jwtService;

    /**
     * Filters each incoming request to validate the JWT and set authentication context.
     *
     * @param exchange the HTTP request/response context
     * @param chain    the filter chain to continue processing
     * @return a reactive Mono that continues the filter chain
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // Read the Authorization header from the request
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // If there's no token or the format is incorrect, skip authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        // Remove "Bearer " prefix to extract the raw token
        String token = authHeader.substring(7);

        // Extract username/email/ID from token (usually stored as subject)
        String username = jwtService.extractUsername(token);

        // If the token is valid and we could extract a user
        if (username != null && jwtService.isTokenValid(token)) {
            // Create a Spring Security User with no roles (authorities)
            User user = new User(username, "", Collections.emptyList());

            // Wrap user in an authentication token
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            // Add authentication to the reactive context so Spring Security knows the user is authenticated
            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
        }

        // If token is invalid, continue without setting security context
        return chain.filter(exchange);
    }
}
