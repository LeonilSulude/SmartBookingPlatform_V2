package leonil.sulude.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.service.ServiceResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ServiceResourceController.
 *
 * These tests validate:
 * - HTTP request mapping
 * - Status codes
 * - JSON response body
 *
 * The Service layer is mocked.
 */
@WebMvcTest(ServiceResourceController.class)
class ServiceResourceControllerTest {

    @Autowired
    private MockMvc mockMvc; // Simulates HTTP requests

    @Autowired
    private ObjectMapper objectMapper; // Converts objects to JSON

    @Autowired
    private ServiceResourceService service; // Mocked service

    /**
     * Tests POST /api/resources
     */
    @Test
    void shouldCreateResource() throws Exception {

        UUID id = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                offerId,
                "Haircut Premium",
                BigDecimal.valueOf(40),
                45,
                true,
                null
        );

        ServiceResourceResponseDTO response = new ServiceResourceResponseDTO(
                id,
                "Haircut Premium",
                BigDecimal.valueOf(40),
                45,
                true,
                java.util.List.of()
        );

        when(service.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/resources")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()) // Expect HTTP 201
                .andExpect(header().string("Location", "/api/resources/" + id))
                .andExpect(jsonPath("$.name").value("Haircut Premium")); // Validate JSON field

        verify(service).create(any()); // Ensure service method was called
    }

    /**
     * Tests GET /api/resources/{id}
     */
    @Test
    void shouldReturnResourceById() throws Exception {

        UUID id = UUID.randomUUID();

        ServiceResourceResponseDTO response = new ServiceResourceResponseDTO(
                id,
                "Massage",
                BigDecimal.valueOf(60),
                60,
                true,
                java.util.List.of()
        );

        when(service.getById(id)).thenReturn(Optional.of(response));

        mockMvc.perform(get("/api/resources/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Massage")); // Validate JSON response

        verify(service).getById(id);
    }

    /**
     * Tests GET /api/resources/{id} when resource does not exist.
     */
    @Test
    void shouldReturn404WhenResourceNotFound() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.getById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/resources/" + id))
                .andExpect(status().isNotFound());

        verify(service).getById(id);
    }

///*    *//**
//     * Tests DELETE /api/resources/{id}
//     *//*
//    @Test
//    void shouldDeleteResource() throws Exception {
//
//        UUID id = UUID.randomUUID();
//
//        mockMvc.perform(delete("/api/resources/" + id))
//                .andExpect(status().isNoContent());
//
//        verify(service).delete(id); // Ensure delete method was called
//    }*/


    /**
     * Tests PATCH /api/resources/{id}/deactivate
     * Verifies that deactivating a resource returns HTTP 200 with the updated resource.
     */
    @Test
    void shouldDeactivateResource() throws Exception {
        UUID id = UUID.randomUUID();
        ServiceResourceResponseDTO response = new ServiceResourceResponseDTO(
                id, "Room 101", BigDecimal.valueOf(99.99), 60, false, List.of()
        );

        when(service.deactivate(id)).thenReturn(Optional.of(response));

        mockMvc.perform(patch("/api/resources/" + id + "/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    /**
     * Tests PATCH /api/resources/{id}/deactivate when resource does not exist.
     * Verifies that HTTP 404 is returned.
     */
    @Test
    void shouldReturn404WhenDeactivatingNonExistentResource() throws Exception {
        UUID id = UUID.randomUUID();

        when(service.deactivate(id)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/resources/" + id + "/deactivate"))
                .andExpect(status().isNotFound());
    }

    /**
     * Test configuration replacing the real service with a Mockito mock.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        ServiceResourceService service() {
            return mock(ServiceResourceService.class);
        }
    }
}