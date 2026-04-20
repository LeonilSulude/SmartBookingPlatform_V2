package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceOfferRequestDTO;
import leonil.sulude.catalog.dto.ServiceOfferResponseDTO;
import leonil.sulude.catalog.model.ServiceCategory;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import leonil.sulude.catalog.repository.ServiceResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceOfferServiceImpl.
 *
 * MockitoExtension automatically creates the mocks
 * and injects them into the class under test.
 */
@ExtendWith(MockitoExtension.class)
class ServiceOfferServiceImplTest {

    @Mock
    private ServiceOfferRepository offerRepository; // Fake repository created by Mockito

    @Mock
    private ServiceResourceRepository resourceRepository; // Fake repository created by Mockito

    @InjectMocks
    private ServiceOfferServiceImpl service; // Real service with the mocks injected

    /**
     * Tests retrieving all offers without resources.
     */
    @Test
    void shouldReturnAllOffersWithoutResources() {

        ServiceOffer offer = new ServiceOffer(
                "Haircut",
                "Professional haircut",
                ServiceCategory.BEAUTY,
                "Salon A",
                "Lisbon"
        );

        // Define behavior: when repository findAll() is called, return this list
        when(offerRepository.findAll()).thenReturn(List.of(offer));

        List<ServiceOfferResponseDTO> result = service.getAll();

        // Validate result
        assertEquals(1, result.size());
        assertEquals("Haircut", result.get(0).title());

        // Verify repository method was called exactly once
        verify(offerRepository).findAll(); // Ensure service called repository.findAll()
    }

    /**
     * Tests retrieving an offer by ID when it exists.
     */
    @Test
    void shouldReturnOfferById() {

        UUID id = UUID.randomUUID();

        ServiceOffer offer = new ServiceOffer(
                "Yoga Class",
                "Morning yoga session",
                ServiceCategory.FITNESS,
                "Yoga Studio",
                "Coimbra"
        );

        // Simulate repository returning the offer
        when(offerRepository.findById(id)).thenReturn(Optional.of(offer));

        // Simulate resource lookup returning empty list
        when(resourceRepository.findByOfferId(any())).thenReturn(List.of());

        Optional<ServiceOfferResponseDTO> result = service.getById(id);

        assertTrue(result.isPresent());
        assertEquals("Yoga Class", result.get().title());

        // Verify repository call
        verify(offerRepository).findById(id); // Check that findById(id) was executed

        // Verify resource lookup also happened
        verify(resourceRepository).findByOfferId(any()); // Check resources were fetched for that offer
    }

    /**
     * Tests retrieving an offer by ID when it does not exist.
     */
    @Test
    void shouldReturnEmptyWhenOfferNotFound() {

        UUID id = UUID.randomUUID();

        // Simulate repository not finding the offer
        when(offerRepository.findById(id)).thenReturn(Optional.empty());

        Optional<ServiceOfferResponseDTO> result = service.getById(id);

        assertTrue(result.isEmpty());

        // Verify repository was called
        verify(offerRepository).findById(id); // Ensure service tried to find the offer

        // Verify resource repository was NEVER called
        verify(resourceRepository, never()).findByOfferId(any());
        // never() means: this method should not be called during this test
    }

    /**
     * Tests creating a new offer.
     */
    @Test
    void shouldCreateOffer() {

        ServiceOfferRequestDTO request = new ServiceOfferRequestDTO(
                "Java Training",
                "Backend development course",
                ServiceCategory.EDUCATION,
                "Tech Academy",
                "Porto",
                null
        );

        ServiceOffer saved = new ServiceOffer(
                request.title(),
                request.description(),
                request.category(),
                request.providerName(),
                request.location()
        );

        // Simulate repository saving the entity
        when(offerRepository.save(any())).thenReturn(saved);
        // any() means: accept any ServiceOffer object passed to save()

        ServiceOfferResponseDTO response = service.create(request);

        assertEquals("Java Training", response.title());

        // Verify repository save was called
        verify(offerRepository).save(any());
        // Check that the service attempted to persist the offer
    }

    /**
     * Tests successful deletion of an offer.
     */
    @Test
    void shouldDeleteOfferWhenExists() {

        UUID id = UUID.randomUUID();

        // Simulate repository confirming that the offer exists
        when(offerRepository.existsById(id)).thenReturn(true);

        boolean result = service.delete(id);

        assertTrue(result);

        // Verify existence check happened
        verify(offerRepository).existsById(id); // Service checked if offer exists

        // Verify delete operation happened
        verify(offerRepository).deleteById(id); // Service executed delete
    }

    /**
     * Tests deletion when the offer does not exist.
     */
    @Test
    void shouldReturnFalseWhenDeletingNonExistingOffer() {

        UUID id = UUID.randomUUID();

        // Simulate repository saying the offer does not exist
        when(offerRepository.existsById(id)).thenReturn(false);

        boolean result = service.delete(id);

        assertFalse(result);

        // Verify existence check happened
        verify(offerRepository).existsById(id); // Service checked if offer exists

        // Verify delete was NEVER executed
        verify(offerRepository, never()).deleteById(any());
        // never() ensures that deleteById() was not called
    }
}