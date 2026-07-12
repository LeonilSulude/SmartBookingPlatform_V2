package leonil.sulude.booking.pact;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.DslPart;
import au.com.dius.pact.consumer.dsl.PactDslJsonArray;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-side HTTP contract for CatalogClient's calls to the Catalog Service's
 * /api/resources endpoints. Generates a pact file (default: target/pacts) that
 * CatalogServiceHttpPactVerificationTest, in catalog-service, replays against the real
 * ServiceResourceController to prove both sides agree on the same contract.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "catalog-service", pactVersion = PactSpecVersion.V3)
class CatalogServiceHttpPactTest {

    private static final String RESOURCE_ID = "e2b1a7d0-1a2b-4c3d-8e9f-0a1b2c3d4e5f";

    @Pact(consumer = "booking-service")
    RequestResponsePact resourceExists(PactDslWithProvider builder) {
        DslPart body = new PactDslJsonBody()
                .stringMatcher("id", "[0-9a-fA-F-]{36}", RESOURCE_ID)
                .stringType("name", "Haircut Deluxe")
                .decimalType("price", 35.00)
                .integerType("durationInMinutes", 45)
                .booleanType("active", true)
                .minArrayLike("unavailablePeriods", 0)
                    .datetime("startTime", "yyyy-MM-dd'T'HH:mm:ss")
                    .datetime("endTime", "yyyy-MM-dd'T'HH:mm:ss")
                .closeArray();

        return builder
                .given("resource " + RESOURCE_ID + " exists")
                .uponReceiving("a request for an existing resource")
                .path("/api/resources/" + RESOURCE_ID)
                .method("GET")
                .willRespondWith()
                .headers(Map.of("Content-Type", "application/json"))
                .status(200)
                .body(body)
                .toPact();
    }

    @Pact(consumer = "booking-service")
    RequestResponsePact resourceDoesNotExist(PactDslWithProvider builder) {
        return builder
                .given("no resource exists with id " + RESOURCE_ID)
                .uponReceiving("a request for a resource that does not exist")
                .path("/api/resources/" + RESOURCE_ID)
                .method("GET")
                .willRespondWith()
                .status(404)
                .toPact();
    }

    @Pact(consumer = "booking-service")
    RequestResponsePact allActiveResources(PactDslWithProvider builder) {
        DslPart body = PactDslJsonArray.arrayMinLike(0)
                .stringMatcher("id", "[0-9a-fA-F-]{36}", RESOURCE_ID)
                .stringType("name", "Haircut Deluxe")
                .decimalType("price", 35.00)
                .integerType("durationInMinutes", 45)
                .booleanType("active", true)
                .minArrayLike("unavailablePeriods", 0)
                    .datetime("startTime", "yyyy-MM-dd'T'HH:mm:ss")
                    .datetime("endTime", "yyyy-MM-dd'T'HH:mm:ss")
                .closeArray()
                .closeObject();

        return builder
                .given("at least one active resource exists")
                .uponReceiving("a request for all active resources")
                .path("/api/resources")
                .method("GET")
                .willRespondWith()
                .headers(Map.of("Content-Type", "application/json"))
                .status(200)
                .body(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "resourceExists")
    void testResourceExists(MockServer mockServer) throws IOException {
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + "/api/resources/" + RESOURCE_ID)
                .execute()
                .returnResponse();
        assertThat(response.getCode()).isEqualTo(200);
    }

    @Test
    @PactTestFor(pactMethod = "resourceDoesNotExist")
    void testResourceDoesNotExist(MockServer mockServer) throws IOException {
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + "/api/resources/" + RESOURCE_ID)
                .execute()
                .returnResponse();
        assertThat(response.getCode()).isEqualTo(404);
    }

    @Test
    @PactTestFor(pactMethod = "allActiveResources")
    void testAllActiveResources(MockServer mockServer) throws IOException {
        ClassicHttpResponse response = (ClassicHttpResponse) Request
                .get(mockServer.getUrl() + "/api/resources")
                .execute()
                .returnResponse();
        assertThat(response.getCode()).isEqualTo(200);
    }
}
