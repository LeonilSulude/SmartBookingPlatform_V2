package leonil.sulude.catalog.service;

import leonil.sulude.catalog.dto.ServiceResourceRequestDTO;
import leonil.sulude.catalog.dto.ServiceResourceResponseDTO;
import leonil.sulude.catalog.dto.UnavailablePeriodDTO;
import leonil.sulude.catalog.model.ServiceOffer;
import leonil.sulude.catalog.model.ServiceResource;
import leonil.sulude.catalog.repository.ServiceOfferRepository;
import leonil.sulude.catalog.repository.ServiceResourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ServiceResourceServiceImpl.
 */
@ExtendWith(MockitoExtension.class)
class ServiceResourceServiceImplTest {

    @Mock
    private ServiceResourceRepository repository; // Mocked resource repository

    @Mock
    private ServiceOfferRepository offerRepository; // Mocked offer repository

    @InjectMocks
    private ServiceResourceServiceImpl service; // Service under test

    /**
     * Tests successful creation of a service resource.
     */
    @Test
    void shouldCreateResourceSuccessfully() {

        UUID offerId = UUID.randomUUID();

        ServiceOffer offer = new ServiceOffer();
        offer.setId(offerId);

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                offerId,
                "Haircut Basic",
                BigDecimal.valueOf(20),
                30,
                true,
                null
        );

        ServiceResource saved = new ServiceResource();
        saved.setOffer(offer);
        saved.setName("Haircut Basic");
        saved.setPrice(BigDecimal.valueOf(20));
        saved.setDurationInMinutes(30);
        saved.setActive(true);
        saved.setUnavailablePeriods(List.of());

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(repository.save(any())).thenReturn(saved);

        ServiceResourceResponseDTO response = service.create(request);

        assertEquals("Haircut Basic", response.name());

        verify(offerRepository).findById(offerId); // Ensure offer lookup happened
        verify(repository).save(any()); // Ensure resource was persisted
    }

    /**
     * Tests creation failure when the service offer does not exist.
     */
    @Test
    void shouldThrowExceptionIfOfferNotFound() {

        UUID offerId = UUID.randomUUID();

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                offerId,
                "Massage",
                BigDecimal.valueOf(50),
                60,
                true,
                null
        );

        when(offerRepository.findById(offerId)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        verify(offerRepository).findById(offerId); // Verify lookup occurred
        verify(repository, never()).save(any()); // Ensure resource was not saved
    }

    /**
     * Tests validation when unavailable period end time is before start time.
     */
    @Test
    void shouldThrowExceptionForInvalidUnavailablePeriod() {

        UUID offerId = UUID.randomUUID();

        ServiceOffer offer = new ServiceOffer();
        offer.setId(offerId);

        UnavailablePeriodDTO period = new UnavailablePeriodDTO(
                LocalDateTime.now(),
                LocalDateTime.now().minusHours(1) // Invalid period
        );

        ServiceResourceRequestDTO request = new ServiceResourceRequestDTO(
                offerId,
                "Therapy Session",
                BigDecimal.valueOf(100),
                60,
                true,
                List.of(period)
        );

        when(offerRepository.findById(offerId)).thenReturn(Optional.of(offer));

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        verify(repository, never()).save(any()); // Resource should not be saved
    }

    /**
     * Tests retrieving resources by offer ID.
     */
    @Test
    void shouldReturnResourcesByOffer() {

        UUID offerId = UUID.randomUUID();

        ServiceResource resource = new ServiceResource();
        resource.setId(UUID.randomUUID());
        resource.setName("Haircut Premium");
        resource.setPrice(BigDecimal.valueOf(40));
        resource.setDurationInMinutes(45);
        resource.setActive(true);
        resource.setUnavailablePeriods(List.of());

        when(repository.findByOfferId(offerId)).thenReturn(List.of(resource));

        List<ServiceResourceResponseDTO> result = service.getByOffer(offerId);

        assertEquals(1, result.size());

        verify(repository).findByOfferId(offerId); // Verify repository lookup
    }

    /**
     * Tests retrieving resource by ID when found.
     */
    @Test
    void shouldReturnResourceById() {

        UUID id = UUID.randomUUID();

        ServiceResource resource = new ServiceResource();
        resource.setId(id);
        resource.setName("Massage Therapy");
        resource.setPrice(BigDecimal.valueOf(60));
        resource.setDurationInMinutes(60);
        resource.setActive(true);
        resource.setUnavailablePeriods(List.of());

        when(repository.findById(id)).thenReturn(Optional.of(resource));

        Optional<ServiceResourceResponseDTO> result = service.getById(id);

        assertTrue(result.isPresent());

        verify(repository).findById(id); // Ensure repository lookup happened
    }

    /**
     * Tests retrieving resource by ID when it does not exist.
     */
    @Test
    void shouldReturnEmptyWhenResourceNotFound() {

        UUID id = UUID.randomUUID();

        when(repository.findById(id)).thenReturn(Optional.empty());

        Optional<ServiceResourceResponseDTO> result = service.getById(id);

        assertTrue(result.isEmpty());

        verify(repository).findById(id); // Ensure lookup happened
    }

    /**
     * Tests deletion of a resource.
     */
    @Test
    void shouldDeleteResource() {

        UUID id = UUID.randomUUID();

        service.delete(id);

        verify(repository).deleteById(id); // Ensure repository delete was called
    }
}