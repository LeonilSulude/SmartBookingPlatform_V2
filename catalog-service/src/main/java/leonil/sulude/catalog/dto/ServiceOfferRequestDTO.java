package leonil.sulude.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import leonil.sulude.catalog.model.ServiceCategory;

import java.util.List;

public record ServiceOfferRequestDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Description is required")
        String description,

        @NotNull(message = "Category is required")
        ServiceCategory category,

        @NotBlank(message = "Provider name is required")
        String providerName,

        @NotBlank(message = "Location is required")
        String location,

        @JsonInclude(JsonInclude.Include.NON_NULL)
        List<ServiceResourceResponse> resources

) {}
