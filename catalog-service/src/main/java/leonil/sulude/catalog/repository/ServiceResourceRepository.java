package leonil.sulude.catalog.repository;

import leonil.sulude.catalog.model.ServiceResource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceResourceRepository extends JpaRepository<ServiceResource, UUID> {
    List<ServiceResource> findByOfferId(UUID offerId);
}
