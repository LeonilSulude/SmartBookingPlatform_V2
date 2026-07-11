package leonil.sulude.catalog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceUpdateDTO;
import leonil.sulude.catalog.model.ServiceCategory;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import org.junit.jupiter.api.BeforeEach;
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
 * Integration tests for the resource lifecycle transitions --- update, deactivate,
 * activate --- each verified to publish the correct Kafka event type with the
 * correct payload, against real PostgreSQL and real Kafka.
 */
class ResourceLifecycleEventIT extends AbstractIntegrationTest {

    private static final String TOPIC = "catalog.resource.events";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ServiceOfferRepository offerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID resourceId;

    @BeforeEach
    void createInitialResource() {
        ServiceOffer offer = new ServiceOffer(
                "Guitar Lessons", "One-on-one guitar tuition",
                ServiceCategory.EDUCATION, "Music Academy", "Porto");
        ServiceOffer savedOffer = offerRepository.save(offer);

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                savedOffer.getId(), "Beginner Slot", BigDecimal.valueOf(20.00), 45, true, List.of());

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/resources", jsonEntity(request), String.class);
        resourceId = extractId(response.getBody());
    }

    @Test
    void shouldPublishResourceUpdatedEventWithNewValues() {
        ServiceResourceUpdateDTO update = new ServiceResourceUpdateDTO(
                "Advanced Slot", BigDecimal.valueOf(35.00), 60);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/resources/" + resourceId, HttpMethod.PUT, jsonEntity(update), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // filters by eventType too, not just resourceId — the same resourceId also
        // appears on the RESOURCE_CREATED event from setup, since it's the same resource
        JsonNode event = findEventFor(resourceId, "RESOURCE_UPDATED");
        assertThat(event.get("name").asText()).isEqualTo("Advanced Slot");
        assertThat(event.get("price").asDouble()).isEqualTo(35.00);
        assertThat(event.get("durationInMinutes").asInt()).isEqualTo(60);
    }

    @Test
    void shouldPublishResourceDeactivatedEventWithActiveFalse() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/resources/" + resourceId + "/deactivate", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode event = findEventFor(resourceId, "RESOURCE_DEACTIVATED");
        assertThat(event.get("active").asBoolean()).isFalse();
    }

    @Test
    void shouldPublishResourceActivatedEventWithActiveTrue() {
        restTemplate.exchange(
                "/api/resources/" + resourceId + "/deactivate", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/resources/" + resourceId + "/activate", HttpMethod.PATCH, HttpEntity.EMPTY, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        // filters out RESOURCE_CREATED and RESOURCE_DEACTIVATED, which also share this resourceId
        JsonNode event = findEventFor(resourceId, "RESOURCE_ACTIVATED");
        assertThat(event.get("active").asBoolean()).isTrue();
    }

    /**
     * Reads every message currently available on the topic and keeps only the one matching
     * both the resource and the event type under test.
     *
     * We do NOT try to control what gets read — every call reads the full topic history
     * available at that point, including events from setup or from earlier assertions in
     * the same test (e.g. RESOURCE_CREATED is still there when this is called from the
     * update/deactivate/activate tests, since it's the same resourceId throughout). Instead
     * of narrowing the read, we narrow what we accept afterwards: filtering on resourceId
     * alone would be ambiguous (multiple event types share the same resourceId), so we also
     * filter on eventType to pick out exactly the one event each test cares about.
     *
     * This avoids relying on Kafka consumer group / offset semantics to "hide" earlier
     * messages — consumeMessages() always uses a fresh, randomly named consumer group per
     * call (see its javadoc), so there is no persisted read position to rely on anyway.
     */
    private JsonNode findEventFor(UUID resourceId, String expectedEventType) {
        return consumeMessages(TOPIC, Duration.ofSeconds(10)).stream()
                .map(this::parseQuietly)
                .filter(node -> resourceId.toString().equals(node.get("resourceId").asText()))
                .filter(node -> expectedEventType.equals(node.get("eventType").asText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "No " + expectedEventType + " Kafka event found for resource " + resourceId));
    }

    private JsonNode parseQuietly(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpEntity<Object> jsonEntity(Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private UUID extractId(String json) {
        return UUID.fromString(json.split("\"id\":\"")[1].split("\"")[0]);
    }
}