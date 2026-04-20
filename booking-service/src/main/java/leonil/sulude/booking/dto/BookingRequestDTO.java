package leonil.sulude.booking.dto;

import jakarta.validation.constraints.*;
import leonil.sulude.booking.model.BookingStatus;
import leonil.sulude.booking.validation.EndAfterStart;

import java.time.LocalDateTime;
import java.util.UUID;

@EndAfterStart
public record BookingRequestDTO(

        @NotNull(message = "ServiceResource ID is required")
        UUID resourceId,

        @NotBlank(message = "Customer name is required")
        String customerName,

        @NotBlank(message = "Customer email is required")
        @Email(message = "Email should be valid")
        String customerEmail,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "End time is required")
        @Future(message = "End time must be in the future")
        LocalDateTime endTime,

        BookingStatus status // optional, defaults to PENDING in the entity
) {}
