package leonil.sulude.booking.integration;

import leonil.sulude.booking.model.ResourceCache;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.model.BookingStatus;
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
 * Integration test for the cache hit path --- the resource is already present in the
 * local resource_cache before the booking request arrives.
 */
class BookingCacheHitIT extends AbstractIntegrationTest {

    // makes real HTTP calls; already knows the RANDOM_PORT assigned at runtime
    @Autowired
    private TestRestTemplate restTemplate;

    // used to pre-populate the cache before the test, simulating a resource
    // that was already created earlier and consumed via Kafka or a previous cache miss
    @Autowired
    private ResourceCacheRepository resourceCacheRepository;

    /**
     * Scenario: cache hit --- the resource already exists in the local cache.
     * Stable data (name, price, duration) must come from the cache, not from Catalog.
     * unavailablePeriods must still come from Catalog --- always real-time, never cached.
     */
    @Test
    void shouldUseCacheForStableDataAndCatalogForAvailability() {
        UUID resourceId = UUID.randomUUID();

        // pre-populate the cache — simulates a resource already known locally
        resourceCacheRepository.save(ResourceCache.builder()
                .id(resourceId)
                .name("Massage Therapy")
                .price(BigDecimal.valueOf(50.00))
                .durationInMinutes(60)
                .active(true)
                .lastUpdated(LocalDateTime.now())
                .build());

        // stub returns deliberately WRONG name/price — a "canary" value.
        // if resolveResource() incorrectly used this Catalog response instead of the cache,
        // these wrong values would leak into the booking response and the assertions below
        // would fail. unavailablePeriods is still read from this stub — that part IS expected
        // to come from Catalog even on a cache hit.
        stubFor(get(urlEqualTo("/api/resources/" + resourceId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "id": "%s",
                                    "name": "WRONG NAME — should not be used",
                                    "price": 999.99,
                                    "durationInMinutes": 999,
                                    "active": true,
                                    "unavailablePeriods": []
                                }
                                """.formatted(resourceId))));

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "John Smith",
                "john@test.com",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(2).plusHours(1),
                BookingStatus.PENDING,
                null
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<BookingRequestDTO> entity = new HttpEntity<>(request, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/bookings", entity, String.class);

        // if these assertions pass, it proves resolveResource() read name/price from the
        // cache — the response does NOT contain the canary values from the Catalog stub above
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("Massage Therapy");
        assertThat(response.getBody()).contains("50.0");
        assertThat(response.getBody()).doesNotContain("WRONG NAME");

        // Catalog was still called once — for unavailablePeriods, never skipped even on cache hit
        verify(1, getRequestedFor(urlEqualTo("/api/resources/" + resourceId)));
    }
}