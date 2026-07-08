package leonil.sulude.booking.integration;

import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.model.BookingStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for booking conflict detection and idempotency key behaviour,
 * using real PostgreSQL via Testcontainers and WireMock to simulate the Catalog Service.
 */
class BookingConflictIT extends AbstractIntegrationTest {

    // makes real HTTP calls; already knows the RANDOM_PORT assigned at runtime
    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Scenario: two bookings requested for the same resource with overlapping time slots.
     * The first succeeds; the second must be rejected with HTTP 409.
     */
    @Test
    void shouldRejectSecondBookingWithOverlappingTime() {
        UUID resourceId = UUID.randomUUID();
        stubActiveResource(resourceId);

        LocalDateTime start = LocalDateTime.now().plusDays(3);
        LocalDateTime end = start.plusHours(1);

        BookingRequestDTO first = new BookingRequestDTO(
                resourceId, "Alice", "alice@test.com", start, end, BookingStatus.PENDING, null);
        BookingRequestDTO second = new BookingRequestDTO(
                resourceId, "Bob", "bob@test.com", start, end, BookingStatus.PENDING, null);

        ResponseEntity<String> firstResponse = post(first);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // same resource, same time slot — must be rejected regardless of different customer
        ResponseEntity<String> secondResponse = post(second);
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    /**
     * Scenario: the same request is sent twice with the same idempotency key,
     * simulating a client retry after a network timeout.
     * The second request must return the original booking, not create a duplicate
     * and not fail with a conflict — the client should never see an error for its own retry.
     */
    @Test
    void shouldReturnOriginalBookingWhenIdempotencyKeyRepeats() {
        UUID resourceId = UUID.randomUUID();
        stubActiveResource(resourceId);

        String idempotencyKey = UUID.randomUUID().toString();
        BookingRequestDTO request = new BookingRequestDTO(
                resourceId, "Carol", "carol@test.com",
                LocalDateTime.now().plusDays(4),
                LocalDateTime.now().plusDays(4).plusHours(1),
                BookingStatus.PENDING,
                idempotencyKey);

        ResponseEntity<String> firstResponse = post(request);
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        // exact same request repeated — as a real client retry would do
        ResponseEntity<String> secondResponse = post(request);

        // same booking returned, not a 409 — the client's own retry must succeed transparently.
        // comparing only the id (not the full JSON body) — LocalDateTime serialization can
        // produce trailing-zero differences in nanoseconds between calls even for the same
        // value, making raw string comparison too brittle for this field
        assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(secondResponse.getBody()).contains(extractId(firstResponse.getBody()));

        // Catalog was called only once — the second request short-circuited on the idempotency
        // key lookup and never reached resolveResource(), confirming no duplicate processing
        verify(1, getRequestedFor(urlEqualTo("/api/resources/" + resourceId)));
    }

    private String extractId(String json) {
        // simple extraction — avoids pulling in a JSON parser just for one field in a test
        return json.split("\"id\":\"")[1].split("\"")[0];
    }

    private void stubActiveResource(UUID resourceId) {
        stubFor(get(urlEqualTo("/api/resources/" + resourceId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "name": "Yoga Class",
                                    "price": 15.00,
                                    "durationInMinutes": 60,
                                    "active": true,
                                    "unavailablePeriods": []
                                }
                                """.formatted(resourceId))));
    }

    private ResponseEntity<String> post(BookingRequestDTO request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return restTemplate.postForEntity("/api/bookings", new HttpEntity<>(request, headers), String.class);
    }
}