package leonil.sulude.catalog.dto;

import java.util.UUID;

public record ServiceResourceResponse(
        UUID id,
        String name,
        boolean active
) {}
