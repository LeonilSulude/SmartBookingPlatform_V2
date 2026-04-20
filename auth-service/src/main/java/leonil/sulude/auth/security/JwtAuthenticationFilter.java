package leonil.sulude.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import leonil.sulude.auth.model.User;
import leonil.sulude.auth.repository.UserRepository;
import leonil.sulude.auth.service.JwtService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * JWT Authentication Filter that intercepts incoming HTTP requests to extract and validate JWT tokens.
 * <p>
 * If a valid token is found in the Authorization header, the filter authenticates the user
 * by loading it from the database and setting the authentication context.
 * </p>
 *
 * <p><strong>Expected header format:</strong> {@code Authorization: Bearer <token>}</p>
 *
 * <p>This filter runs once per request and works alongside Spring Security to secure protected routes.</p>
 *
 * @author Leonil
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    /**
     * Constructs the filter with required dependencies.
     *
     * @param jwtService      Service to validate and parse JWT tokens
     * @param userRepository  Repository to fetch user details from the database
     */
    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Intercepts incoming HTTP requests to:
     * <ul>
     *   <li>Check for a valid Authorization header</li>
     *   <li>Validate the JWT token</li>
     *   <li>Load the corresponding user and set the SecurityContext</li>
     * </ul>
     *
     * @param request     The incoming HTTP request
     * @param response    The HTTP response
     * @param filterChain The filter chain to continue the request
     * @throws ServletException if an error occurs while processing the request
     * @throws IOException      if an input or output error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // If no token is provided or the format is incorrect, skip authentication
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // The Authorization header has the format: "Bearer <JWT_TOKEN>"
        // This line strips the "Bearer " part and keeps only the JWT token itself
        final String token = authHeader.substring(7);


        // If token is invalid (expired, malformed, etc.), skip authentication
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract user ID (subject) from the token and fetch the user from the database
        String userIdString = jwtService.extractClaims(token).getSubject();
        UUID userId = UUID.fromString(userIdString);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for ID: " + userId));

        // Create an authentication object for Spring Security
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );

        authentication.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );

        // Set the authenticated user in the security context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
