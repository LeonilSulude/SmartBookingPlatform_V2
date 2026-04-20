package leonil.sulude.booking.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.booking.controller.BookingController;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.exception.BookingConflictException;
import leonil.sulude.booking.exception.GlobalExceptionHandler;
import leonil.sulude.booking.exception.ResourceUnavailableException;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone tests for GlobalExceptionHandler.
 *
 * NOTE:
 * These tests do not start the full Spring context.
 * Instead they test controller + exception handler in isolation.
 *
 * Advantages:
 * - Faster tests
 * - Focused validation of error responses
 */
class GlobalExceptionHandlerStandaloneTest {

    private MockMvc mockMvc;

    private BookingService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        service = Mockito.mock(BookingService.class);

        BookingController controller = new BookingController(service);

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();

        objectMapper = new ObjectMapper();
    }

    /**
     * Tests validation errors triggered by @Valid annotations.
     */
    @Test
    void shouldReturn400WhenValidationFails() throws Exception {

        String invalidJson = """
        {
          "resourceId": null,
          "customerName": "",
          "customerEmail": "invalid",
          "startTime": "2030-01-01T10:00:00",
          "endTime": "2030-01-01T09:00:00"
        }
        """;

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"));
    }

    /**
     * Tests database integrity violation handling.
     */
    @Test
    void shouldReturn400WhenDataIntegrityViolationOccurs() throws Exception {

        when(service.create(any()))
                .thenThrow(new DataIntegrityViolationException("not-null constraint"));

        String validJson = """
        {
          "resourceId": "%s",
          "customerName": "John",
          "customerEmail": "john@test.com",
          "startTime": "2030-01-01T10:00:00",
          "endTime": "2030-01-01T11:00:00"
        }
        """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Data Integrity Violation"));
    }

    /**
     * Tests booking conflict exception handling.
     */
    @Test
    void shouldReturn409WhenBookingConflictOccurs() throws Exception {

        when(service.create(any()))
                .thenThrow(new BookingConflictException("Booking overlap"));

        String validJson = """
        {
          "resourceId": "%s",
          "customerName": "John",
          "customerEmail": "john@test.com",
          "startTime": "2030-01-01T10:00:00",
          "endTime": "2030-01-01T11:00:00"
        }
        """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Booking Conflict"));
    }

    /**
     * Tests resource unavailable exception handling.
     */
    @Test
    void shouldReturn503WhenResourceUnavailable() throws Exception {

        when(service.create(any()))
                .thenThrow(new ResourceUnavailableException("Resource inactive"));

        String validJson = """
        {
          "resourceId": "%s",
          "customerName": "John",
          "customerEmail": "john@test.com",
          "startTime": "2030-01-01T10:00:00",
          "endTime": "2030-01-01T11:00:00"
        }
        """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Resource Unavailable"));
    }

    /**
     * Tests generic fallback exception handling.
     */
    @Test
    void shouldReturn500ForUnexpectedErrors() throws Exception {

        when(service.getAll()).thenThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}