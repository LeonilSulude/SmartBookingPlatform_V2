package leonil.sulude.e2e;

import io.restassured.http.ContentType;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared helpers for e2e test setup — registering users and creating offers/resources.
 * Kept as plain static methods rather than a base class with @BeforeEach, since different
 * tests need different combinations (some need two providers, some need none at all).
 */
final class E2ETestSupport {

    private E2ETestSupport() {}

    static String registerAndGetToken(String role) {
        String email = role.toLowerCase() + "-" + UUID.randomUUID() + "@e2e.test";
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "E2E %s",
                            "email": "%s",
                            "password": "securePassword123",
                            "role": "%s"
                        }
                        """.formatted(role, email, role))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().path("token");
    }

    static String createOffer(String providerToken) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + providerToken)
                .body("""
                        {
                            "title": "E2E Offer %s",
                            "description": "Support-created offer",
                            "category": "FITNESS",
                            "providerName": "E2E Provider",
                            "location": "Lisbon"
                        }
                        """.formatted(UUID.randomUUID()))
                .when()
                .post("/api/offers")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    static String createResource(String providerToken, String offerId, double price, int durationMinutes) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + providerToken)
                .body("""
                        {
                            "offerId": "%s",
                            "name": "E2E Resource %s",
                            "price": %s,
                            "durationInMinutes": %d,
                            "active": true,
                            "unavailablePeriods": []
                        }
                        """.formatted(offerId, UUID.randomUUID(), price, durationMinutes))
                .when()
                .post("/api/resources")
                .then()
                .statusCode(201)
                .extract().path("id");
    }
}