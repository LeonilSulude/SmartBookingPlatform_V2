package leonil.sulude.auth.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.UUID;

/**
 * Reads X-Correlation-Id from incoming requests and stores it in MDC.
 * Uses OncePerRequestFilter — correct interface for servlet-based (Spring MVC) services.
 * WebFilter is WebFlux only and must not be used here.
 */
@Component
public class CorrelationIdPropagationFilter extends OncePerRequestFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String correlationId = request.getHeader(CORRELATION_HEADER);

        // fall back to a new UUID if the gateway did not provide one
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // store in MDC so all log statements in this request include the correlation ID
        MDC.put(MDC_KEY, correlationId);

        // propagate to response for client traceability
        response.addHeader(CORRELATION_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // always clear MDC after request — prevents thread pool contamination
            MDC.remove(MDC_KEY);
        }
    }
}