package leonil.sulude.catalog.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.catalog.controller.ServiceOfferController;
import leonil.sulude.catalog.service.ServiceOfferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Standalone tests for GlobalExceptionHandler.
 *
 * This test setup uses MockMvcBuilders.standaloneSetup() instead of loading
 * the full Spring Boot context, making tests faster and more focused.
 *
 * It validates how exceptions are translated into HTTP responses.
 */
class GlobalExceptionHandlerStandaloneTest {

    private MockMvc mockMvc;

    private ServiceOfferService service;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {

        // Mock service used by the controller
        service = Mockito.mock(ServiceOfferService.class);

        // Controller under test
        ServiceOfferController controller = new ServiceOfferController(service);

        // Validator required for @Valid annotations to work in standalone mode
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
     * Tests validation errors triggered by Bean Validation (@Valid).
     */
    @Test
    void shouldReturn400WhenValidationFails() throws Exception {

        String invalidJson = """
        {
          "title": "",
          "description": "Test",
          "category": "EDUCATION",
          "providerName": "Academy",
          "location": "Porto"
        }
        """;

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("One or more fields are invalid."));
    }

    /**
     * Tests handling of database integrity violations.
     */
    @Test
    void shouldReturn400WhenDataIntegrityViolationOccurs() throws Exception {

        when(service.create(any()))
                .thenThrow(new DataIntegrityViolationException("not-null constraint"));

        String validJson = """
        {
          "title": "Java Training",
          "description": "Backend course",
          "category": "EDUCATION",
          "providerName": "Tech Academy",
          "location": "Porto"
        }
        """;

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Data Integrity Violation"));
    }

    /**
     * Tests IllegalArgumentException raised by application logic.
     */
    @Test
    void shouldReturn400WhenIllegalArgumentOccurs() throws Exception {

        when(service.create(any()))
                .thenThrow(new IllegalArgumentException("Invalid category"));

        String json = """
        {
          "title": "Test",
          "description": "Test",
          "category": "EDUCATION",
          "providerName": "Academy",
          "location": "Lisbon"
        }
        """;

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid input"));
    }

    /**
     * Tests malformed JSON payloads that cannot be parsed.
     */
    @Test
    void shouldReturn400ForMalformedJson() throws Exception {

        String malformedJson = """
        {
          "title": "Test",
          "description": "Test",
          "category": "EDUCATION",
          "providerName": "Academy",
          "location": "Lisbon",
        }
        """; // trailing comma breaks JSON

        mockMvc.perform(post("/api/offers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Malformed request"));
    }

    /**
     * Tests generic exception fallback returning HTTP 500.
     */
    @Test
    void shouldReturn500ForUnexpectedErrors() throws Exception {

        when(service.getAll())
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(get("/api/offers"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Internal Server Error"));
    }
}