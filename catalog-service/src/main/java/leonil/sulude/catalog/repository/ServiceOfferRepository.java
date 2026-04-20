package leonil.sulude.catalog.repository;

import leonil.sulude.catalog.model.ServiceOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ServiceOfferRepository extends JpaRepository<ServiceOffer, UUID> {
}
