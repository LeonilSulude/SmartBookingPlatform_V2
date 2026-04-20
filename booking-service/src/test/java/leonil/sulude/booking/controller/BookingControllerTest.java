package leonil.sulude.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import leonil.sulude.booking.controller.BookingController;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookingController.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService service; // mocked service

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Resets mock interactions before each test.
     * Ensures tests remain independent from each other.
     */
    @BeforeEach
    void resetMocks() {
        Mockito.reset(service);
    }

    /**
     * Tests retrieving all bookings.
     */
    @Test
    void shouldReturnAllBookings() throws Exception {

        BookingResponseDTO booking = new BookingResponseDTO(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "John Doe",
                "john@test.com",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                BookingStatus.PENDING,
                LocalDateTime.now(),
                "Haircut",
                null,
                null
        );

        when(service.getAll()).thenReturn(List.of(booking));

        mockMvc.perform(get("/api/bookings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("John Doe"));
    }

    /**
     * Tests retrieving a booking by ID when it exists.
     */
    @Test
    void shouldReturnBookingById() throws Exception {

        UUID id = UUID.randomUUID();

        BookingResponseDTO booking = new BookingResponseDTO(
                id,
                UUID.randomUUID(),
                "Alice",
                "alice@test.com",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                BookingStatus.PENDING,
                LocalDateTime.now(),
                "Massage",
                null,
                null
        );

        when(service.getById(id)).thenReturn(Optional.of(booking));

        mockMvc.perform(get("/api/bookings/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerName").value("Alice"));
    }

    /**
     * Tests retrieving a booking that does not exist.
     */
    @Test
    void shouldReturn404WhenBookingNotFound() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.getById(id)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/bookings/" + id))
                .andExpect(status().isNotFound());
    }

    /**
     * Tests successful booking creation.
     */
    @Test
    void shouldCreateBookingSuccessfully() throws Exception {

        UUID resourceId = UUID.randomUUID();

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "Bob",
                "bob@test.com",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                null
        );

        BookingResponseDTO response = new BookingResponseDTO(
                UUID.randomUUID(),
                resourceId,
                "Bob",
                "bob@test.com",
                request.startTime(),
                request.endTime(),
                BookingStatus.PENDING,
                LocalDateTime.now(),
                "Yoga",
                null,
                null
        );

        when(service.create(Mockito.any())).thenReturn(response);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Bob"));
    }

    /**
     * Tests successful deletion of a booking.
     */
    @Test
    void shouldDeleteBooking() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.delete(id)).thenReturn(true);

        mockMvc.perform(delete("/api/bookings/" + id))
                .andExpect(status().isNoContent());
    }

    /**
     * Tests deleting a booking that does not exist.
     */
    @Test
    void shouldReturn404WhenDeletingNonExistingBooking() throws Exception {

        UUID id = UUID.randomUUID();

        when(service.delete(id)).thenReturn(false);

        mockMvc.perform(delete("/api/bookings/" + id))
                .andExpect(status().isNotFound());
    }

    /**
     * Test configuration replacing the real service with a Mockito mock.
     */
    @TestConfiguration
    static class TestConfig {

        @Bean
        BookingService bookingService() {
            return Mockito.mock(BookingService.class);
        }
    }
}
