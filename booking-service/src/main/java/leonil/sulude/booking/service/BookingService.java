package leonil.sulude.booking.service;

import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingService {

    /** returns all bookings */
    List<BookingResponseDTO> getAll();

    /** returns a booking by id, or empty if not found */
    Optional<BookingResponseDTO> getById(UUID id);

    /** creates a new booking */
    BookingResponseDTO create(BookingRequestDTO booking);

    /** deletes a booking by id, returns false if not found */
    boolean delete(UUID id);

    /** cancels a booking; idempotent if already cancelled; enforces the 30-min cutoff */
    Optional<BookingResponseDTO> cancel(UUID id);
}