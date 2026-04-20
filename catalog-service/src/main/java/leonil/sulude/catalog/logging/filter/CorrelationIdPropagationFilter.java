package leonil.sulude.catalog.logging.filter;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * CorrelationIdPropagationFilter
 *
 * PURPOSE:
 * --------
 * Reads correlation ID from incoming HTTP requests
 * and stores it in MDC for logging consistency.
 *
 * NOTE:
 * -----
 * This service does NOT generate correlation IDs.
 * The API Gateway is responsible for generation.
 *
 * This filter simply propagates it internally
 * for consistent logging within this service.
 */
@Component
public class CorrelationIdPropagationFilter implements WebFilter {

    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // Store in MDC for logging
        MDC.put(MDC_KEY, finalCorrelationId);

        // Expose it in response for traceability
        exchange.getResponse()
                .getHeaders()
                .add(CORRELATION_HEADER, finalCorrelationId);

        // Clear MDC after request completes (ThreadLocal safety)
        return chain.filter(exchange)
                .doFinally(signalType -> MDC.remove(MDC_KEY));
    }
}

