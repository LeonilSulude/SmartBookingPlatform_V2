package leonil.sulude.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import leonil.sulude.catalog.model.ServiceCategory;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

// Excludes null and empty values from JSON responses (e.g. empty lists or empty strings),
// keeping the API responses clean and avoiding unnecessary fields.
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ServiceOfferResponseDTO(
        UUID id,
        String title,
        String description,
        ServiceCategory category,
        String providerName,
        String location,
        List<ServiceResourceResponseDTO> resources // se for incluído
) {}
