package leonil.sulude.catalog.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.service.ServiceResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;

@Tag(
        name = "Catalog - Service Resources",
        description = "Endpoints for managing service resources linked to service offers"
)
@RestController
@RequestMapping("/api/resources")
public class ServiceResourceController {

    private final ServiceResourceService service;

    public ServiceResourceController(ServiceResourceService service) {
        this.service = service;
    }

    /**
     * Creates a new service resource.
     *
     * @param dto Request body containing resource details
     * @return The created resource and location header
     */
    @Operation(
            summary = "Create a service resource",
            description = "Creates a new resource associated with a service offer."
    )
    @ApiResponse(responseCode = "201", description = "Resource created successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    @PostMapping
    public ResponseEntity<ServiceResourceResponseDTO> create(@Valid @RequestBody ServiceResourceRequestDTO dto) {
        ServiceResourceResponseDTO created = service.create(dto);
        URI location = URI.create("/api/resources/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Retrieves a service resource by ID.
     *
     * @param id Resource ID
     * @return The corresponding resource or 404 if not found
     */
    @Operation(
            summary = "Retrieve a service resource by ID",
            description = "Returns details for a specific service resource."
    )
    @ApiResponse(responseCode = "200", description = "Resource found")
    @ApiResponse(responseCode = "404", description = "Resource not found")
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResourceResponseDTO> getById(@PathVariable UUID id) {
        return service.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Deletes a resource by its ID.
     *
     * @param id Resource ID
     * @return 204 No Content on success
     */
    @Operation(
            summary = "Delete a service resource",
            description = "Deletes a service resource by its identifier."
    )
    @ApiResponse(responseCode = "204", description = "Resource deleted successfully")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}