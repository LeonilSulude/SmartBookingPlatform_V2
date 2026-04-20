package leonil.sulude.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.dto.ServiceOfferRequestDTO;
import leonil.sulude.catalog.dto.ServiceOfferResponseDTO;
import leonil.sulude.catalog.model.ServiceCategory;
import leonil.sulude.catalog.service.ServiceOfferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ServiceOfferController.
 *
 * These tests validate the HTTP layer:
 * - Request mapping
 * - Status codes
 * - JSON responses
 *
 * The Service layer is mocked.
 */
@WebMvcTest(ServiceOfferController.class)
class ServiceOfferControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // Converts objects to JSON

    @Autowired
    private ServiceOfferService service; // Mocked service

    /**
     * Tests GET /api/offers without resources.
     */
    @Test
    void shouldReturnAllOffersWithoutResources() throws Exception {

        ServiceOfferResponseDTO response = new ServiceOfferResponseDTO(
                UUID.randomUUID(),
                "Haircut",
                "Basic haircut",
                ServiceCategory.BEAUTY,
                "Salon A",
                "Lisbon",
                null
        );

        when(service.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Haircut")); // Navigate JSON root ($) → field "title" and assert its value

        verify(service).getAll(); // Ensure correct service method was called
    }

    /**
     * Tests GET /api/offers?includeResources=true
     */
    @Test
    void shouldReturnOffersWithResources() throws Exception {

        ServiceOfferResponseDTO response = new ServiceOfferResponseDTO(
                UUID.randomUUID(),
                "Massage",
                "Relaxing massage",
                ServiceCategory.HEALTH,
                "Spa Center",
                "Porto",
                List.of()
        );

        when(service.getAllWithResources()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/offers")
                        .param("includeResources", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Massage")); // Navigate JSON root ($) → field "title" and assert its value

        verify(service).getAllWithResources();
    }

    /**
     * Tests GET /api/offers/{id} when offer exists.
     */
    @Test
    void shouldReturnOfferById() throws Exception {

        UUID id = UUID.randomUUID();

        ServiceOfferResponseDTO response = new ServiceOfferResponseDTO(
                id,
                "Yoga Class",
                "Morning yoga",
                ServiceCategory.FITNESS,
                "Yoga Studio",
                "Coimbra",
                null
        );

        when(service.getById(id)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/offers/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Yoga Class")); // Navigate JSON root ($) → field "title" and assert its value

        verify(service).getById(id);
    }

    /**
     * Tests GET /api/offers/{id} when offer does not exist.
     */
    @Test
    void shouldReturn404WhenOfferNotFound() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.getById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/offers/" + id))
                .andExpect(status().isNotFound());

        verify(service).getById(id);
    }

    /**
     * Tests POST /api/offers
     */
    @Test
    void shouldCreateOffer() throws Exception {

        UUID id = UUID.randomUUID();

        ServiceOfferRequestDTO request = new ServiceOfferRequestDTO(
                "Java Course",
                "Backend training",
                ServiceCategory.EDUCATION,
                "Tech School",
                "Lisbon",
                null
        );

        ServiceOfferResponseDTO response = new ServiceOfferResponseDTO(
                id,
                "Java Course",
                "Backend training",
                ServiceCategory.EDUCATION,
                "Tech School",
                "Lisbon",
                null
        );

        when(service.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/offers")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Controller returns 201
                .andExpect(header().string("Location", "/api/offers/" + id))
                .andExpect(jsonPath("$.title").value("Java Course"));

        verify(service).create(any());
    }

    /**
     * Tests DELETE /api/offers/{id} when offer exists.
     */
    @Test
    void shouldDeleteOffer() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/offers/" + id))
                .andExpect(status().isNoContent());

        verify(service).delete(id);
    }

    /**
     * Tests DELETE /api/offers/{id} when offer does not exist.
     */
    @Test
    void shouldReturn404WhenDeletingMissingOffer() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/offers/" + id))
                .andExpect(status().isNotFound());

        verify(service).delete(id);
    }

    /**
     * Test configuration that replaces the real service with a Mockito mock.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        ServiceOfferService service() {
            return mock(ServiceOfferService.class);
        }
    }
}