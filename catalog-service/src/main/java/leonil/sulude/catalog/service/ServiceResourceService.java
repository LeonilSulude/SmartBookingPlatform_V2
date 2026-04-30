package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.dto.ServiceResourceUpdateDTO;
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
     * Returns all active resources — used by Booking Service on startup to seed the local cache.
     *
     * @return list of active resources
     */
    List<ServiceResourceResponseDTO> getAllActive();

/*    *//**
     * Deletes a service resource by its unique ID.
     *
     * @param id The ID of the resource to delete.
     *//*
    void delete(UUID id);*/

    /**
     * Deactivates a resource by ID — soft delete.
     *
     * @param id the resource ID
     * @return the updated resource, or empty if not found
     */
    Optional<ServiceResourceResponseDTO> deactivate(UUID id);

    /**
     * Activates a previously deactivated resource.
     * Publishes RESOURCE_UPDATED event so the Booking Service cache reflects the new active state.
     *
     * @param id the resource ID
     * @return the updated resource, or empty if not found
     */
    Optional<ServiceResourceResponseDTO> activate(UUID id);

    /**
     * Updates stable data of an existing resource — name, price, duration.
     * Publishes RESOURCE_UPDATED event to Kafka so the Booking Service cache is refreshed.
     *
     * @param id  the resource ID
     * @param dto the update request
     * @return the updated resource, or empty if not found
     */
    Optional<ServiceResourceResponseDTO> update(UUID id, ServiceResourceUpdateDTO dto);
}
