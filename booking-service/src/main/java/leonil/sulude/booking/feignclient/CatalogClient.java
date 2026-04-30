package leonil.sulude.booking.feignclient;

import leonil.sulude.booking.dto.ServiceResourceResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "catalog-service" // name = spring.application.name of the other service
)

public interface CatalogClient {

    @GetMapping("/api/resources/{id}")
    ServiceResourceResponseDTO getResourceById(@PathVariable UUID id);

    /** fetches all active resources — called once on startup to seed the local resource cache */
    @GetMapping("/api/resources")
    List<ServiceResourceResponseDTO> getAllActiveResources();
}
