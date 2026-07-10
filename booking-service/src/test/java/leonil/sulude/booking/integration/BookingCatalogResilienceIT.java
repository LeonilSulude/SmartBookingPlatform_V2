package leonil.sulude.booking.integration;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.model.BookingStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for resilience when the Catalog Service is unavailable.
 * Simulates a hard failure (connection reset) via WireMock's fault injection,
 * validating that Resilience4j's retry + circuit breaker + fallback chain produces
 * a clear HTTP 503 rather than an unhandled 500 or an indefinite hang.
 *
 * Unlike a unit test that mocks CatalogClient to throw directly, this exercises the
 * real Feign call, the real Resilience4j interceptors configured in application.yaml,
 * and the real fallback method wiring --- proving the resilience configuration itself
 * is correct, not just the fallback logic in isolation.
 */
class BookingCatalogResilienceIT extends AbstractIntegrationTest {

    // makes real HTTP calls; already knows the RANDOM_PORT assigned at runtime
    @Autowired
    private TestRestTemplate restTemplate;

    // used to reset the circuit breaker after deliberately tripping it —
    // Spring caches this ApplicationContext across IT classes with identical configuration,
    // so this bean (and its internal state) is SHARED with other test classes. Without an
    // explicit reset, tripping the breaker here left it OPEN for BookingConflictIT, which ran
    // next alphabetically and got 503s on perfectly healthy WireMock stubs — the breaker was
    // short-circuiting every call before it ever reached the (healthy) stub.
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @AfterEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("catalogService").reset();
    }

    @Test
    void shouldReturn503WhenCatalogIsUnavailableOnCacheMiss() {
        UUID resourceId = UUID.randomUUID();

        // simulates the Catalog Service being down --- connection reset, not a clean HTTP error.
        // this is what a real outage looks like at the network level, as opposed to a 4xx/5xx
        // response, which would be a much simpler (and less realistic) failure to simulate
        stubFor(get(urlEqualTo("/api/resources/" + resourceId))
                .willReturn(aResponse().withFault(com.github.tomakehurst.wiremock.http.Fault.CONNECTION_RESET_BY_PEER)));

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId, "Frank", "frank@test.com",
                LocalDateTime.now().plusDays(6),
                LocalDateTime.now().plusDays(6).plusHours(1),
                BookingStatus.PENDING,
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings", new HttpEntity<>(request, headers), String.class);

        // Resilience4j retries a few times against the broken stub, then the circuit breaker's
        // fallback throws ResourceUnavailableException, mapped by GlobalExceptionHandler to 503 ---
        // this is the resilience chain from Section 5 of the report, now proven end-to-end
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);

        // Catalog was called more than once — proof the retry mechanism actually engaged,
        // not just that a single failed call short-circuited straight to the fallback
        verify(moreThan(1), getRequestedFor(urlEqualTo("/api/resources/" + resourceId)));
    }
}