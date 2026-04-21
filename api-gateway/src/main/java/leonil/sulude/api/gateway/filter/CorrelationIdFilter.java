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
 * Generates or propagates X-Correlation-Id for every incoming request.
 * Stores it in Reactor Context instead of MDC — MDC is thread-local and
 * unsafe in WebFlux where a request can be processed across multiple threads.
 * Reactor Context travels with the reactive pipeline regardless of thread switches.
 */
@Slf4j
@Component
public class CorrelationIdFilter implements WebFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        // use existing ID if provided by client or upstream system, otherwise generate
        String correlationId = exchange.getRequest()
                .getHeaders()
                .getFirst(CORRELATION_HEADER);

        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;

        // mutate request to forward correlation ID to downstream services
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_HEADER, finalCorrelationId))
                .build();

        // add to response so clients can reference it
        mutatedExchange.getResponse()
                .getHeaders()
                .add(CORRELATION_HEADER, finalCorrelationId);

        // propagate via Reactor Context — safe across thread switches unlike MDC
        return chain.filter(mutatedExchange)
                .contextWrite(ctx -> ctx.put(CORRELATION_KEY, finalCorrelationId));
    }
}