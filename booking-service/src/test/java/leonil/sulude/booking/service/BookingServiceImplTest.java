package leonil.sulude.booking.service;

import leonil.sulude.booking.client.CatalogResourceClient;
import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import leonil.sulude.booking.dto.UnavailablePeriodDTO;
import leonil.sulude.booking.exception.BookingCancellationNotAllowedException;
import leonil.sulude.booking.exception.BookingConflictException;
import leonil.sulude.booking.exception.ResourceUnavailableException;
import leonil.sulude.booking.model.Booking;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.repository.BookingRepository;
import leonil.sulude.booking.repository.ResourceCacheRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookingServiceImpl.
 *
 * These tests validate the business logic of booking creation, cancellation,
 * conflict detection, and deletion using mocked dependencies.
 *
 * Mocks CatalogResourceClient, not CatalogClient directly — CatalogResourceClient
 * is the bean that wraps the Feign call with Resilience4j (@Retry/@CircuitBreaker).
 * It was extracted into its own class specifically because Spring's proxy-based AOP
 * cannot intercept self-invocations: keeping fetchResource() as a private method inside
 * BookingServiceImpl meant the resilience annotations were silently never applied —
 * discovered via an integration test simulating a real Catalog outage.
 */
class BookingServiceImplTest {

    @Mock
    private BookingRepository repository;

    @Mock
    private CatalogResourceClient catalogResourceClient;

    @Mock
    private ResourceCacheRepository resourceCacheRepository; // mocked cache repository

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

        // no overlapping bookings exist for the requested time slot
        when(repository.existsOverlappingBooking(any(), any(), any()))
                .thenReturn(false);

        // simulate cache miss — forces fallback to Catalog via CatalogResourceClient
        when(resourceCacheRepository.findById(any())).thenReturn(Optional.empty());

        ServiceResourceResponseDTO resource =
                new ServiceResourceResponseDTO(
                        resourceId,
                        "Haircut",
                        new BigDecimal("25.00"),
                        30,
                        true,
                        List.of()
                );

        when(catalogResourceClient.fetchResource(resourceId))
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

        when(resourceCacheRepository.findById(any())).thenReturn(Optional.empty());

        ServiceResourceResponseDTO resource =
                new ServiceResourceResponseDTO(
                        resourceId,
                        "Haircut",
                        new BigDecimal("25"),
                        30,
                        false,
                        List.of()
                );

        when(catalogResourceClient.fetchResource(resourceId))
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

        when(resourceCacheRepository.findById(any())).thenReturn(Optional.empty());

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

        when(catalogResourceClient.fetchResource(resourceId))
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

    /**
     * Tests successful cancellation when the booking is far enough in the future.
     */
    @Test
    void shouldCancelBookingSuccessfully() {

        UUID id = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Booking booking = new Booking();
        booking.setId(id);
        booking.setResourceId(resourceId);
        booking.setStatus(BookingStatus.PENDING);
        booking.setStartTime(LocalDateTime.now().plusDays(1)); // well outside the 30-min window

        when(repository.findById(id)).thenReturn(Optional.of(booking));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(resourceCacheRepository.findById(resourceId)).thenReturn(Optional.empty());
        when(catalogResourceClient.fetchResource(resourceId)).thenReturn(
                new ServiceResourceResponseDTO(resourceId, "Yoga", new BigDecimal("15"), 60, true, List.of()));

        Optional<BookingResponseDTO> result = service.cancel(id);

        assertTrue(result.isPresent());
        assertEquals(BookingStatus.CANCELLED, result.get().status());
        verify(repository).save(argThat(b -> b.getStatus() == BookingStatus.CANCELLED));
    }

    /**
     * Tests that cancelling an already-cancelled booking is idempotent —
     * returns the booking as-is, does not throw, does not call save() again.
     */
    @Test
    void shouldBeIdempotentWhenCancellingAlreadyCancelledBooking() {

        UUID id = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        Booking booking = new Booking();
        booking.setId(id);
        booking.setResourceId(resourceId);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setStartTime(LocalDateTime.now().plusDays(1));

        when(repository.findById(id)).thenReturn(Optional.of(booking));
        when(resourceCacheRepository.findById(resourceId)).thenReturn(Optional.empty());
        when(catalogResourceClient.fetchResource(resourceId)).thenReturn(
                new ServiceResourceResponseDTO(resourceId, "Yoga", new BigDecimal("15"), 60, true, List.of()));

        Optional<BookingResponseDTO> result = service.cancel(id);

        assertTrue(result.isPresent());
        assertEquals(BookingStatus.CANCELLED, result.get().status());
        verify(repository, never()).save(any()); // idempotent path never writes
    }

    /**
     * Tests that cancellation is rejected within the 30-minute cutoff before start time.
     */
    @Test
    void shouldThrowBookingCancellationNotAllowedWhenWithinCancellationWindow() {

        UUID id = UUID.randomUUID();

        Booking booking = new Booking();
        booking.setId(id);
        booking.setResourceId(UUID.randomUUID());
        booking.setStatus(BookingStatus.PENDING);
        booking.setStartTime(LocalDateTime.now().plusMinutes(10)); // inside the 30-min window

        when(repository.findById(id)).thenReturn(Optional.of(booking));

        assertThrows(
                BookingCancellationNotAllowedException.class,
                () -> service.cancel(id)
        );

        verify(repository, never()).save(any());
    }

    /**
     * Tests that cancelling a non-existent booking returns empty, not an exception.
     */
    @Test
    void shouldReturnEmptyWhenCancellingNonExistentBooking() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<BookingResponseDTO> result = service.cancel(id);

        assertTrue(result.isEmpty());
        verify(repository, never()).save(any());
    }
}