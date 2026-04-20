package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.dto.UnavailablePeriodDTO;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.model.ServiceResource;
import leonil.sulude.catalog.model.UnavailablePeriod;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import leonil.sulude.catalog.repository.ServiceResourceRepository;
import leonil.sulude.catalog.service.ServiceResourceService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ServiceResourceServiceImpl implements ServiceResourceService {

    private final ServiceResourceRepository repository;
    private final ServiceOfferRepository offerRepository;

    public ServiceResourceServiceImpl(ServiceResourceRepository repository, ServiceOfferRepository offerRepository) {
        this.repository = repository;
        this.offerRepository = offerRepository;
    }

    /**
     * Creates a new resource associated with a service offer.
     *
     * @param dto The incoming resource creation request DTO.
     * @return The created resource as a response DTO.
     */
    @Override
    public ServiceResourceResponseDTO create(ServiceResourceRequestDTO dto) {
        ServiceOffer offer = offerRepository.findById(dto.offerId())
                .orElseThrow(() -> new IllegalArgumentException("ServiceOffer not found: " + dto.offerId()));

        ServiceResource resource = new ServiceResource();
        resource.setOffer(offer);
        resource.setName(dto.name());
        resource.setPrice(dto.price());
        resource.setDurationInMinutes(dto.durationInMinutes());
        resource.setActive(dto.active());

        if (dto.unavailablePeriods() != null) {
            List<UnavailablePeriod> periods = dto.unavailablePeriods().stream()
                    .peek(p -> {
                        if (p.startTime() == null || p.endTime() == null) {
                            throw new IllegalArgumentException("Start and end time are required for unavailable period.");
                        }
                        if (p.endTime().isBefore(p.startTime())) {
                            throw new IllegalArgumentException("End time must be after start time for unavailable period.");
                        }
                    })
                    .map(p -> new UnavailablePeriod(p.startTime(), p.endTime()))
                    .toList();
            resource.setUnavailablePeriods(periods);
        }

        ServiceResource saved = repository.save(resource);
        return toResponseDTO(saved);
    }

    /**
     * Returns a list of resources linked to a specific service offer.
     *
     * @param offerId The offer ID.
     * @return List of response DTOs.
     */
    @Override
    public List<ServiceResourceResponseDTO> getByOffer(UUID offerId) {
        return repository.findByOfferId(offerId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    /**
     * Gets a resource by ID.
     *
     * @param id The resource ID.
     * @return Optional containing the response DTO if found.
     */
    @Override
    public Optional<ServiceResourceResponseDTO> getById(UUID id) {
        return repository.findById(id)
                .map(this::toResponseDTO);
    }

    /**
     * Deletes a resource by ID.
     *
     * @param id The ID to delete.
     */
    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    /**
     * Helper method to convert entity to DTO.
     */
    private ServiceResourceResponseDTO toResponseDTO(ServiceResource entity) {
        return new ServiceResourceResponseDTO(
                entity.getId(),
                entity.getName(),
                entity.getPrice(),
                entity.getDurationInMinutes(),
                entity.isActive(),
                entity.getUnavailablePeriods().stream()
                        .map(p -> new UnavailablePeriodDTO(p.getStartTime(), p.getEndTime()))
                        .toList()
        );
    }
}
