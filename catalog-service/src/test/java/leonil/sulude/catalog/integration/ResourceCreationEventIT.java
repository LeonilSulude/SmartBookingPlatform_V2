package leonil.sulude.catalog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.model.ServiceCategory;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that creating a resource publishes a well-formed
 * RESOURCE_CREATED event to Kafka --- using real PostgreSQL (the resource is actually
 * persisted) and real Kafka (the event is actually on the topic, not just asserted
 * against a mocked KafkaTemplate as a unit test would).
 */
class ResourceCreationEventIT extends AbstractIntegrationTest {

    private static final String TOPIC = "catalog.resource.events";

    // makes real HTTP calls; already knows the RANDOM_PORT assigned at runtime
    @Autowired
    private TestRestTemplate restTemplate;

    // creates the parent offer directly — the create-resource scenario under test
    // doesn't need to exercise the offer-creation endpoint itself
    @Autowired
    private ServiceOfferRepository offerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldPublishResourceCreatedEventWithCorrectPayload() throws Exception {
        ServiceOffer offer = new ServiceOffer(
                "Deep Tissue Massage", "60-minute therapeutic massage",
                ServiceCategory.HEALTH, "Zen Studio", "Lisbon");
        ServiceOffer savedOffer = offerRepository.save(offer);

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                savedOffer.getId(),
                "Massage Room 1",
                BigDecimal.valueOf(45.00),
                60,
                true,
                List.of()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/resources", new HttpEntity<>(request, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode responseBody = objectMapper.readTree(response.getBody());
        UUID resourceId = UUID.fromString(responseBody.get("id").asText());

        // read the topic and find the event for this specific resource — the topic may
        // contain events from other tests since containers (and the topic) are shared
        List<String> messages = consumeMessages(TOPIC, Duration.ofSeconds(10));
        JsonNode event = messages.stream()
                .map(this::parseQuietly)
                .filter(node -> resourceId.toString().equals(node.get("resourceId").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No Kafka event found for resource " + resourceId));

        assertThat(event.get("eventType").asText()).isEqualTo("RESOURCE_CREATED");
        assertThat(event.get("name").asText()).isEqualTo("Massage Room 1");
        assertThat(event.get("price").asDouble()).isEqualTo(45.00);
        assertThat(event.get("durationInMinutes").asInt()).isEqualTo(60);
        assertThat(event.get("active").asBoolean()).isTrue();
    }

    private JsonNode parseQuietly(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}