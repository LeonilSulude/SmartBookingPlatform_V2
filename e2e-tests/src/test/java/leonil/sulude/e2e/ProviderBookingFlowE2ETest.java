package leonil.sulude.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * The core business flow of the platform, end-to-end, through the real API Gateway,
 * with every microservice, database, and message broker running for real:
 *
 * 1. A PROVIDER registers and logs in
 * 2. The PROVIDER creates an offer and a resource under it
 * 3. A CLIENT registers and logs in
 * 4. The CLIENT books that resource
 * 5. The booking is confirmed and visible with the resource's name/price
 *
 * This is the one test in the whole suite that can catch a genuine end-to-end
 * regression that no isolated service test could: e.g. a mismatch between what
 * Catalog actually publishes to Kafka and what Booking actually expects to
 * deserialize --- something the informal DTO contract between the two services
 * (ResourceEventDTO / ResourceCacheEventDTO) does not verify automatically today.
 * Formal verification of that contract is what Pact (planned next) is for; this
 * test is the manual, full-stack equivalent in the meantime.
 */
class ProviderBookingFlowE2ETest extends BaseE2ETest {

    @Test
    void shouldCompleteFullProviderToClientBookingFlow() {
        String providerEmail = "provider-" + UUID.randomUUID() + "@e2e.test";
        String clientEmail = "client-" + UUID.randomUUID() + "@e2e.test";

        // 1. Register and log in as PROVIDER
        String providerToken =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "name": "E2E Provider",
                                    "email": "%s",
                                    "password": "securePassword123",
                                    "role": "PROVIDER"
                                }
                                """.formatted(providerEmail))
                        .when()
                        .post("/api/auth/register")
                        .then()
                        .statusCode(201)
                        .extract().path("token");

        // 2. PROVIDER creates an offer
        String offerId =
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + providerToken)
                        .body("""
                                {
                                    "title": "E2E Yoga Class",
                                    "description": "End-to-end test offer",
                                    "category": "FITNESS",
                                    "providerName": "E2E Provider",
                                    "location": "Lisbon"
                                }
                                """)
                        .when()
                        .post("/api/offers")
                        .then()
                        .statusCode(201)
                        .extract().path("id");

        // 3. PROVIDER creates a resource under that offer
        String resourceId =
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + providerToken)
                        .body("""
                                {
                                    "offerId": "%s",
                                    "name": "E2E Morning Slot",
                                    "price": 18.00,
                                    "durationInMinutes": 45,
                                    "active": true,
                                    "unavailablePeriods": []
                                }
                                """.formatted(offerId))
                        .when()
                        .post("/api/resources")
                        .then()
                        .statusCode(201)
                        .extract().path("id");

        // 4. Register and log in as CLIENT
        String clientToken =
                given()
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "name": "E2E Client",
                                    "email": "%s",
                                    "password": "securePassword123",
                                    "role": "CLIENT"
                                }
                                """.formatted(clientEmail))
                        .when()
                        .post("/api/auth/register")
                        .then()
                        .statusCode(201)
                        .extract().path("token");

        // 5. CLIENT books the resource
        String startTime = LocalDateTime.now().plusDays(2)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String endTime = LocalDateTime.now().plusDays(2).plusMinutes(45)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        String bookingId =
                given()
                        .contentType(ContentType.JSON)
                        .header("Authorization", "Bearer " + clientToken)
                        .body("""
                                {
                                    "resourceId": "%s",
                                    "customerName": "E2E Client",
                                    "customerEmail": "%s",
                                    "startTime": "%s",
                                    "endTime": "%s"
                                }
                                """.formatted(resourceId, clientEmail, startTime, endTime))
                        .when()
                        .post("/api/bookings")
                        .then()
                        .statusCode(201)
                        // proves the Kafka event from Catalog reached Booking's cache
                        // and was actually used to populate these fields
                        .body("resourceName", equalTo("E2E Morning Slot"))
                        .body("resourcePrice", equalTo(18.0f))
                        .body("status", equalTo("PENDING"))
                        .extract().path("id");

        // 6. CLIENT retrieves the booking — confirms it's genuinely persisted, not just
        // a response artifact from the create() call
        given()
                .header("Authorization", "Bearer " + clientToken)
                .when()
                .get("/api/bookings/" + bookingId)
                .then()
                .statusCode(200)
                .body("id", equalTo(bookingId))
                .body("resourceName", equalTo("E2E Morning Slot"));
    }
}