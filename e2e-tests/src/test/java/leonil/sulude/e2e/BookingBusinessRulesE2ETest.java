package leonil.sulude.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static leonil.sulude.e2e.E2ETestSupport.*;

/**
 * End-to-end validation of cross-service business rules --- scenarios that only
 * a full-stack test can genuinely prove, since they depend on the real interaction
 * between the Gateway's RBAC, the Booking Service's conflict detection, and the
 * Catalog Service's resource state, all through real HTTP calls and a real Kafka event.
 */
class BookingBusinessRulesE2ETest extends BaseE2ETest {

    @Test
    void shouldRejectSecondBookingForSameResourceAndOverlappingTime() {
        String providerToken = registerAndGetToken("PROVIDER");
        String offerId = createOffer(providerToken);
        String resourceId = createResource(providerToken, offerId, 20.00, 30);

        String clientToken = registerAndGetToken("CLIENT");
        String start = LocalDateTime.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String end = LocalDateTime.now().plusDays(3).plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String bookingBody = """
                {
                    "resourceId": "%s",
                    "customerName": "E2E Client",
                    "customerEmail": "client@e2e.test",
                    "startTime": "%s",
                    "endTime": "%s"
                }
                """.formatted(resourceId, start, end);

        // first booking succeeds
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + clientToken)
                .body(bookingBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(201);

        // second booking for the exact same resource and time slot must be rejected
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + clientToken)
                .body(bookingBody)
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(409);
    }

    @Test
    void shouldRejectProviderAttemptingToCreateBooking() {
        String providerToken = registerAndGetToken("PROVIDER");
        String offerId = createOffer(providerToken);
        String resourceId = createResource(providerToken, offerId, 15.00, 30);

        String start = LocalDateTime.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String end = LocalDateTime.now().plusDays(4).plusMinutes(30).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // a PROVIDER token attempting the CLIENT-only booking creation action —
        // this exercises the real fixed RBAC rule end-to-end, not just at the gateway
        // in isolation as RoleBasedAuthorizationIT already does
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + providerToken)
                .body("""
                        {
                            "resourceId": "%s",
                            "customerName": "Should Not Work",
                            "customerEmail": "provider@e2e.test",
                            "startTime": "%s",
                            "endTime": "%s"
                        }
                        """.formatted(resourceId, start, end))
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldRejectBookingOnDeactivatedResource() {
        String providerToken = registerAndGetToken("PROVIDER");
        String offerId = createOffer(providerToken);
        String resourceId = createResource(providerToken, offerId, 25.00, 60);

        // PROVIDER deactivates the resource — publishes RESOURCE_DEACTIVATED to Kafka
        given()
                .header("Authorization", "Bearer " + providerToken)
                .when()
                .patch("/api/resources/" + resourceId + "/deactivate")
                .then()
                .statusCode(200);

        String clientToken = registerAndGetToken("CLIENT");
        String start = LocalDateTime.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String end = LocalDateTime.now().plusDays(5).plusMinutes(60).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        // CLIENT attempts to book the now-deactivated resource. Note: there is a small,
        // real window here where Booking's cache may not yet reflect the deactivation if
        // the Kafka event hasn't been consumed by the time this request arrives — this
        // test implicitly also validates that the event propagates fast enough in practice
        given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + clientToken)
                .body("""
                        {
                            "resourceId": "%s",
                            "customerName": "E2E Client",
                            "customerEmail": "client2@e2e.test",
                            "startTime": "%s",
                            "endTime": "%s"
                        }
                        """.formatted(resourceId, start, end))
                .when()
                .post("/api/bookings")
                .then()
                .statusCode(503); // ResourceUnavailableException -> 503, per GlobalExceptionHandler
    }
}