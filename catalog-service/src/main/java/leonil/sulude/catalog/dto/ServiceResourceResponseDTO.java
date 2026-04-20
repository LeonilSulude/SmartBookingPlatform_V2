package leonil.sulude.catalog.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ServiceResourceResponseDTO(
        UUID id,
        String name,
        BigDecimal price,
        Integer durationInMinutes,
        boolean active,
        List<UnavailablePeriodDTO> unavailablePeriods
) {}
