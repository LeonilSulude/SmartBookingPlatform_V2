package leonil.sulude.booking.service;

import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.dto.UnavailablePeriodDTO;
import leonil.sulude.booking.exception.BookingConflictException;
import leonil.sulude.booking.exception.ResourceUnavailableException;
import leonil.sulude.booking.feignclient.CatalogClient;
import leonil.sulude.booking.model.Booking;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImpl.
 *
 * These tests validate the business logic of booking creation,
 * conflict detection, and deletion using mocked dependencies.
 */
class BookingServiceImplTest {

    @Mock
    private BookingRepository repository;

    @Mock
    private CatalogClient catalogClient;

    @InjectMocks
    private BookingServiceImpl service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Tests successful booking creation when no conflicts exist.
     */
    @Test
    void shouldCreateBookingSuccessfully() {

        UUID resourceId = UUID.randomUUID();

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "John Doe",
                "john@test.com",
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusHours(2),
                BookingStatus.PENDING,
                null
        );

        when(repository.existsOverlappingBooking(any(), any(), any()))
                .thenReturn(false);

        ServiceResourceResponseDTO resource =
                new ServiceResourceResponseDTO(
                        resourceId,
                        "Haircut",
                        new BigDecimal("25.00"),
                        30,
                        true,
                        List.of()
                );

        when(catalogClient.getResourceById(resourceId))
                .thenReturn(resource);

        Booking saved = new Booking();
        saved.setId(UUID.randomUUID());
        saved.setResourceId(resourceId);
        saved.setCustomerName(request.customerName());
        saved.setCustomerEmail(request.customerEmail());
        saved.setStartTime(request.startTime());
        saved.setEndTime(request.endTime());
        saved.setCreatedAt(LocalDateTime.now());

        when(repository.save(any())).thenReturn(saved);

        BookingResponseDTO response = service.create(request);

        assertNotNull(response);
        assertEquals("John Doe", response.customerName());

        verify(repository).save(any());
    }

    /**
     * Tests that a booking conflict is detected when the
     * requested time overlaps an existing booking.
     */
    @Test
    void shouldThrowBookingConflictExceptionWhenTimeOverlap() {

        BookingRequestDTO request = new BookingRequestDTO(
                UUID.randomUUID(),
                "John",
                "john@test.com",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                BookingStatus.PENDING,
                null
        );

        when(repository.existsOverlappingBooking(any(), any(), any()))
                .thenReturn(true);

        assertThrows(
                BookingConflictException.class,
                () -> service.create(request)
        );

        verify(repository, never()).save(any());
    }

    /**
     * Tests that booking fails if the resource is inactive.
     */
    @Test
    void shouldThrowResourceUnavailableWhenResourceInactive() {

        UUID resourceId = UUID.randomUUID();

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "John",
                "john@test.com",
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1),
                BookingStatus.PENDING,
                null
        );

        when(repository.existsOverlappingBooking(any(), any(), any()))
                .thenReturn(false);

        ServiceResourceResponseDTO resource =
                new ServiceResourceResponseDTO(
                        resourceId,
                        "Haircut",
                        new BigDecimal("25"),
                        30,
                        false,
                        List.of()
                );

        when(catalogClient.getResourceById(resourceId))
                .thenReturn(resource);

        assertThrows(
                ResourceUnavailableException.class,
                () -> service.create(request)
        );
    }

    /**
     * Tests that booking fails when the requested time
     * conflicts with an unavailable period.
     */
    @Test
    void shouldThrowResourceUnavailableWhenUnavailablePeriodConflict() {

        UUID resourceId = UUID.randomUUID();

        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(1);

        BookingRequestDTO request = new BookingRequestDTO(
                resourceId,
                "John",
                "john@test.com",
                start,
                end,
                BookingStatus.PENDING,
                null
        );

        when(repository.existsOverlappingBooking(any(), any(), any()))
                .thenReturn(false);

        UnavailablePeriodDTO period =
                new UnavailablePeriodDTO(
                        start.minusMinutes(10),
                        end.plusMinutes(10)
                );

        ServiceResourceResponseDTO resource =
                new ServiceResourceResponseDTO(
                        resourceId,
                        "Haircut",
                        new BigDecimal("20"),
                        30,
                        true,
                        List.of(period)
                );

        when(catalogClient.getResourceById(resourceId))
                .thenReturn(resource);

        assertThrows(
                ResourceUnavailableException.class,
                () -> service.create(request)
        );
    }

    /**
     * Tests successful deletion when booking exists.
     */
    @Test
    void shouldDeleteBookingWhenExists() {

        UUID id = UUID.randomUUID();

        when(repository.existsById(id)).thenReturn(true);

        boolean result = service.delete(id);

        assertTrue(result);

        verify(repository).deleteById(id);
    }

    /**
     * Tests deletion when booking does not exist.
     */
    @Test
    void shouldReturnFalseWhenDeletingNonExistingBooking() {

        UUID id = UUID.randomUUID();

        when(repository.existsById(id)).thenReturn(false);

        boolean result = service.delete(id);

        assertFalse(result);

        verify(repository, never()).deleteById(any());
    }
}