package leonil.sulude.catalog.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ServiceResourceRequestDTO(
        @NotNull UUID offerId,

        @NotBlank String name,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
        BigDecimal price,

        @Min(value = 1, message = "Duration must be at least 1 minute")
        Integer durationInMinutes,

        boolean active,

        List<UnavailablePeriodDTO> unavailablePeriods
) {}
