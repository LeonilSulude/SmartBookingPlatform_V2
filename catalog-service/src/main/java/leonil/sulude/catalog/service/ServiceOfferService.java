package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceOfferRequestDTO;
import leonil.sulude.catalog.dto.ServiceOfferResponseDTO;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing service offers in the catalog.
 */
public interface ServiceOfferService {

    /**
     * Retrieves all available service offers.
     *
     * @return A list of all ServiceOffer entities.
     */
    List<ServiceOfferResponseDTO> getAll();

    /**
     * Retrieves all available service offers.
     *
     * @return A list of all ServiceOffer entities with the associated service resources.
     */
    List<ServiceOfferResponseDTO> getAllWithResources();

    /**
     * Retrieves a specific service offer by its unique ID.
     *
     * @param id The ID of the service offer.
     * @return An Optional containing the offer if found, or empty otherwise.
     */
    Optional<ServiceOfferResponseDTO> getById(UUID id);

    /**
     * Creates and persists a new service offer based on the provided DTO.
     *
     * @param offer The data transfer object containing the offer details.
     * @return The newly created ServiceOffer entity.
     */
    ServiceOfferResponseDTO create(ServiceOfferRequestDTO offer);

    /**
     * Deletes a service offer by its unique ID.
     *
     * @param id The ID of the offer to delete.
     * @return true if the deletion was successful, false otherwise.
     */
    boolean delete(UUID id);
}
