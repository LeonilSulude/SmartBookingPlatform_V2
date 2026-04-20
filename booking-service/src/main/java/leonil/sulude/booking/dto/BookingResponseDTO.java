package leonil.sulude.booking.dto;

import leonil.sulude.booking.model.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingResponseDTO(
        UUID id,
        UUID serviceResourceId,
        String customerName,
        String customerEmail,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BookingStatus status,
        LocalDateTime createdAt,
        String resourceName,
        BigDecimal resourcePrice,
        Integer resourceDuration
) {}
