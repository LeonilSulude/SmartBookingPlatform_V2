package leonil.sulude.booking.repository;

import leonil.sulude.booking.model.ResourceCache;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Repository for the local resource cache.
 * Used by BookingServiceImpl to avoid synchronous calls to Catalog for stable data.
 */
public interface ResourceCacheRepository extends JpaRepository<ResourceCache, UUID> {
}