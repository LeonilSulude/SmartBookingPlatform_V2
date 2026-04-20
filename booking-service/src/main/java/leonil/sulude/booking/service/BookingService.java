package leonil.sulude.booking.service;

import leonil.sulude.booking.dto.BookingRequestDTO;
import leonil.sulude.booking.dto.BookingResponseDTO;
import leonil.sulude.booking.model.Booking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingService {
    List<BookingResponseDTO> getAll();
    Optional<BookingResponseDTO> getById(UUID id);
    BookingResponseDTO create(BookingRequestDTO booking);
    boolean delete(UUID id);
}
