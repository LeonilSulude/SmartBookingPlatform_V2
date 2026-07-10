package leonil.sulude.booking.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.exception.ResourceUnavailableException;
import leonil.sulude.booking.feignclient.CatalogClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Wraps calls to the Catalog Service with Resilience4j retry and circuit breaker.
 *
 * This MUST be a separate bean from BookingServiceImpl, not a private/internal method there.
 * Spring implements @Retry and @CircuitBreaker via a dynamic proxy wrapped around the bean.
 * The proxy only intercepts calls that arrive from OUTSIDE the bean --- a self-invocation
 * (BookingServiceImpl calling its own method via `this`) bypasses the proxy entirely, silently
 * ignoring the annotations. This was discovered via BookingCatalogResilienceIT: with fetchResource()
 * previously defined inside BookingServiceImpl, a real Catalog outage produced an unhandled 500
 * instead of the expected 503 --- retry and circuit breaker were never actually engaging.
 */
@Component
@RequiredArgsConstructor
public class CatalogResourceClient {

    private final CatalogClient catalogClient;

    /**
     * Retrieves resource data from the Catalog Service.
     * Protected by Resilience4j retry and circuit breaker mechanisms
     * to improve reliability of inter-service communication.
     */
    @CircuitBreaker(name = "catalogService", fallbackMethod = "catalogFallback")
    @Retry(name = "catalogService")
    public ServiceResourceResponseDTO fetchResource(UUID resourceId) {
        return catalogClient.getResourceById(resourceId);
    }

    /**
     * Fallback method executed when the Catalog Service is unavailable
     * after retries or when the circuit breaker is open.
     */
    public ServiceResourceResponseDTO catalogFallback(UUID resourceId, Throwable ex) {
        throw new ResourceUnavailableException("Catalog service unavailable");
    }
}