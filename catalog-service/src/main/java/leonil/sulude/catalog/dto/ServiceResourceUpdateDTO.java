package leonil.sulude.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO for updating stable resource data — name, price, and duration.
 * offerId is excluded — a resource cannot be reassigned to a different offer.
 * active is excluded — use PATCH /deactivate for deactivation.
 */
public record ServiceResourceUpdateDTO(
        @NotBlank String name,

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        BigDecimal price,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationInMinutes
) {}