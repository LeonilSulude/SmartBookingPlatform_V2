package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.model.ServiceResource;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing service resources associated with a service offer.
 */
public interface ServiceResourceService {

    /**
     * Creates and persists a new service resource based on the provided DTO.
     *
     * @param dto The data transfer object containing the resource details.
     * @return The newly created ServiceResource entity.
     */
    ServiceResourceResponseDTO create(ServiceResourceRequestDTO dto);

    /**
     * Retrieves all service resources associated with a specific service offer.
     *
     * @param offerId The ID of the related service offer.
     * @return A list of matching service resources.
     */
    List<ServiceResourceResponseDTO> getByOffer(UUID offerId);

    /**
     * Retrieves a specific service resource by its unique ID.
     *
     * @param id The ID of the service resource.
     * @return An Optional containing the resource if found, or empty otherwise.
     */
    Optional<ServiceResourceResponseDTO> getById(UUID id);

    /**
     * Deletes a service resource by its unique ID.
     *
     * @param id The ID of the resource to delete.
     */
    void delete(UUID id);
}
