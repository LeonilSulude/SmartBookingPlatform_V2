package leonil.sulude.api.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Lightweight test controller used exclusively for gateway filter testing.
 *
 * Purpose:
 * The API Gateway normally forwards requests to downstream microservices.
 * However, when testing gateway filters (e.g., CorrelationIdFilter),
 * we do not want to depend on external services being available.
 *
 * This controller provides a simple local endpoint that allows requests
 * to pass through the gateway filter chain during tests.
 *
 * This enables validation of filter behavior (such as header propagation)
 * without invoking real downstream services.
 */
@RestController
public class CorrelationTestController {

    /**
     * Simple endpoint used only for testing the filter pipeline.
     *
     * The response body itself is irrelevant — the important part
     * is that the request goes through the gateway filters so that
     * headers like "X-Correlation-Id" can be generated or propagated.
     */
    @GetMapping("/test")
    public Mono<String> test() {
        return Mono.just("OK");
    }
}