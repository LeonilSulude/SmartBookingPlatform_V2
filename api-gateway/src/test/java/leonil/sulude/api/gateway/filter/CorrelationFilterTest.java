package leonil.sulude.api.gateway.filter;

import leonil.sulude.api.gateway.CorrelationTestController;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * WebFlux slice test that loads only the controller and filter layer.
 * This avoids starting the full gateway and prevents calls to real microservices,
 * allowing the CorrelationIdFilter to be tested in isolation.
 */
@WebFluxTest(controllers = CorrelationTestController.class)
@Import(CorrelationIdFilter.class)
class CorrelationFilterTest {

    @Autowired
    private WebTestClient webTestClient;

    /**
     * Verifies that the filter generates a correlation ID
     * when the incoming request does not contain one.
     */
    @Test
    void shouldGenerateCorrelationIdWhenNotProvided() {

        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().exists("X-Correlation-Id");
    }

    /**
     * Verifies that an existing correlation ID is preserved
     * and propagated through the filter chain.
     */
    @Test
    void shouldPropagateExistingCorrelationId() {

        webTestClient.get()
                .uri("/test")
                .header("X-Correlation-Id", "test-correlation-123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Correlation-Id", "test-correlation-123");
    }
}