package leonil.sulude.booking.pact;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.messaging.Message;
import au.com.dius.pact.core.model.messaging.MessagePact;
import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.booking.dto.ResourceCacheEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Consumer-side message contract for the "catalog.resource.events" Kafka topic that
 * ResourceEventConsumer listens to. Generates a pact file (default: target/pacts) that
 * ResourceEventsMessagePactVerificationTest, in catalog-service, replays against the real
 * ResourceEventProducer to prove Catalog actually publishes what Booking expects.
 *
 * providerType = ASYNCH marks this as a message (not HTTP request/response) pact, per
 * pact-jvm's own AsyncMessageTest reference example.
 */
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "catalog-service-kafka-resource-events", providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V3)
class CatalogResourceEventsMessagePactTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Pact(consumer = "booking-service")
    MessagePact resourceCreatedEvent(MessagePactBuilder builder) {
        PactDslJsonBody body = new PactDslJsonBody()
                .stringMatcher("eventType",
                        "RESOURCE_CREATED|RESOURCE_UPDATED|RESOURCE_ACTIVATED|RESOURCE_DEACTIVATED",
                        "RESOURCE_CREATED")
                .uuid("resourceId")
                .stringType("name", "Haircut Deluxe")
                .decimalType("price", 35.00)
                .integerType("durationInMinutes", 45)
                .booleanType("active", true);

        return builder
                .given("a resource was created in Catalog")
                .expectsToReceive("a resource created event")
                .withMetadata(Map.of("contentType", "application/json"))
                .withContent(body)
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "resourceCreatedEvent")
    void testResourceCreatedEvent(List<Message> messages) throws Exception {
        Message message = messages.get(0);

        // proves ResourceEventConsumer.consume(ResourceCacheEventDTO) can actually
        // deserialize what the pact says Catalog will publish
        ResourceCacheEventDTO event = objectMapper.readValue(message.contentsAsBytes(), ResourceCacheEventDTO.class);

        assertThat(event.eventType()).isEqualTo("RESOURCE_CREATED");
        assertThat(event.resourceId()).isNotNull();
        assertThat(event.name()).isEqualTo("Haircut Deluxe");
        assertThat(event.price()).isEqualByComparingTo("35.00");
        assertThat(event.durationInMinutes()).isEqualTo(45);
        assertThat(event.active()).isTrue();
    }
}
