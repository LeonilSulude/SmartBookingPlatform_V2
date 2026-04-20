package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.*;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.model.ServiceResource;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import leonil.sulude.catalog.repository.ServiceResourceRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ServiceOfferServiceImpl implements ServiceOfferService {

    private final ServiceOfferRepository offerRepository;
    private final ServiceResourceRepository resourceRepository;

    public ServiceOfferServiceImpl(ServiceOfferRepository offerRepository,
                                   ServiceResourceRepository resourceRepository) {
        this.offerRepository = offerRepository;
        this.resourceRepository = resourceRepository;
    }

    @Override
    public List<ServiceOfferResponseDTO> getAll() {
        return offerRepository.findAll().stream()
                .map(this::mapToResponseWithoutResources)
                .toList();
    }

    @Override
    public List<ServiceOfferResponseDTO> getAllWithResources() {
        return offerRepository.findAll().stream()
                .map(offer -> {
                    List<ServiceResource> resources = resourceRepository.findByOfferId(offer.getId());
                    return mapToResponseWithResources(offer, resources);
                })
                .toList();
    }

    @Override
    public Optional<ServiceOfferResponseDTO> getById(UUID id) {
        return offerRepository.findById(id)
                .map(offer -> {
                    List<ServiceResource> resources = resourceRepository.findByOfferId(offer.getId());
                    return mapToResponseWithResources(offer, resources);
                });
    }



    @Override
    public ServiceOfferResponseDTO create(ServiceOfferRequestDTO request) {
        ServiceOffer offer = new ServiceOffer(
                request.title(),
                request.description(),
                request.category(),
                request.providerName(),
                request.location()
        );
        ServiceOffer saved = offerRepository.save(offer);
        return mapToResponseWithoutResources(saved);
    }

    @Override
    public boolean delete(UUID id) {
        if (!offerRepository.existsById(id)) return false;
        offerRepository.deleteById(id);
        return true;
    }

    // --------------------------
    // DTO Mapping Helpers
    // --------------------------

    /**
     * Maps a ServiceOffer entity to a ServiceOfferResponseDTO
     * without including any associated service resources.
     *
     * This is useful when listing offers without needing full detail,
     * improving performance and reducing payload size.
     */
    private ServiceOfferResponseDTO mapToResponseWithoutResources(ServiceOffer offer) {
        return new ServiceOfferResponseDTO(
                offer.getId(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getCategory(),
                offer.getProviderName(),
                offer.getLocation(),
                null // No resources included in this version
        );
    }


    /**
     * Maps a ServiceOffer and its associated ServiceResource entities
     * to a complete ServiceOfferResponseDTO including all relevant details.
     *
     * This method is used when full offer information is required,
     * such as in detailed views or filtered endpoints with resources.
     */
    private ServiceOfferResponseDTO mapToResponseWithResources(ServiceOffer offer, List<ServiceResource> resources) {

        // Convert each ServiceResource entity into a DTO
        List<ServiceResourceResponseDTO> resourceDTOs = resources.stream()
                .map(resource -> {

                    // Map unavailable periods for each resource into DTOs
                    List<UnavailablePeriodDTO> periods = resource.getUnavailablePeriods().stream()
                            .map(p -> new UnavailablePeriodDTO(p.getStartTime(), p.getEndTime()))
                            .toList();

                    // Build and return the resource DTO
                    return new ServiceResourceResponseDTO(
                            resource.getId(),
                            resource.getName(),
                            resource.getPrice(),
                            resource.getDurationInMinutes(),
                            resource.isActive(),
                            periods
                    );
                })
                .toList();

        // Build and return the full offer DTO with all resources
        return new ServiceOfferResponseDTO(
                offer.getId(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getCategory(),
                offer.getProviderName(),
                offer.getLocation(),
                resourceDTOs
        );
    }

}
