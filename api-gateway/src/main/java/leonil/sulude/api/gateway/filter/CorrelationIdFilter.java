package leonil.sulude.api.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * CorrelationIdFilter
 *
 * PURPOSE:
 * --------
 * Ensures every incoming HTTP request contains a correlation ID.
 *
 * WHY THIS MATTERS:
 * -----------------
 * - Enables distributed tracing across microservices
 * - Allows log-service to group logs by request flow
 * - Essential for debugging in distributed systems
 *
 * HOW IT WORKS:
 * -------------
 * 1. Checks if the request already contains "X-Correlation-Id"
 * 2. If not, generates a new UUID
 * 3. Stores the value in MDC (thread-local logging context)
 * 4. Adds the header to the response
 * 5. Clears MDC after request completes (critical to avoid leakage)
 *
 * IMPORTANT:
 * ----------
 * MDC uses ThreadLocal internally.
 * Since threads are reused, failing to clear MDC may cause
 * correlation IDs to leak between unrelated requests.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // 1. Extract correlation ID from request header
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_HEADER);

        // 2. Generate one if missing
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        // 3. Store in MDC (thread-local logging context)
        MDC.put("correlationId", correlationId);

        // 4. Add to response header (so clients can see it)
        exchange.getResponse()
                .getHeaders()
                .add(CORRELATION_HEADER, correlationId);

        // 5. Continue filter chain
        return chain.filter(exchange)
                // 6. Always clear MDC to prevent thread pollution
                .doFinally(signalType -> MDC.clear());
    }
}
