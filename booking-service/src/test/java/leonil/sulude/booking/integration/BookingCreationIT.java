package leonil.sulude.booking.integration;

import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for booking creation, using real PostgreSQL and Kafka via Testcontainers.
 * The Catalog Service is simulated with WireMock --- the Booking Service is tested in isolation.
 * IT suffix (not Test) marks this as an integration test, run only by the failsafe plugin.
 */
class BookingCreationIT extends AbstractIntegrationTest {

    // makes real HTTP calls; already knows the RANDOM_PORT assigned at runtime
    @Autowired
    private TestRestTemplate restTemplate;

    // verifies the cache side effect in the real DB, not just the HTTP response
    @Autowired
    private ResourceCacheRepository resourceCacheRepository;

    /**
     * Scenario: cache miss --- the resource is not yet in the local cache.
     * The Booking Service should call Catalog (via WireMock), create the booking,
     * and populate the cache for future requests.
     */
    @Test
    void shouldCreateBookingOnCacheMiss() {
        UUID resourceId = UUID.randomUUID();

        // stub the Catalog Service response — simulates a real, active, available resource
        stubFor(get(urlEqualTo("/api/resources/" + resourceId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "name": "Haircut Deluxe",
                                    "price": 35.00,
                                    "durationInMinutes": 45,
                                    "active": true,
                                    "unavailablePeriods": []
                                }
                                """.formatted(resourceId))));

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "Jane Doe",
                "jane@test.com",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(1).plusHours(1),
                BookingStatus.PENDING,
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings", entity, String.class);

        // booking was created successfully
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Haircut Deluxe");
        assertThat(response.getBody()).contains("35.0");

        // cache was populated as a side effect of the cache miss
        assertThat(resourceCacheRepository.findById(resourceId)).isPresent();
        var cached = resourceCacheRepository.findById(resourceId).get();
        assertThat(cached.getName()).isEqualTo("Haircut Deluxe");
        assertThat(cached.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(35.00));

        // Catalog was called exactly once — proof this was a genuine cache miss
        verify(1, getRequestedFor(urlEqualTo("/api/resources/" + resourceId)));
    }
}