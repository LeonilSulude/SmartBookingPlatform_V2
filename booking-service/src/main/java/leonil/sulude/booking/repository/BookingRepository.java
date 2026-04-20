package leonil.sulude.booking.repository;

import leonil.sulude.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
    SELECT COUNT(b) > 0 FROM Booking b
    WHERE b.resourceId = :resourceId
      AND (
            (:startTime < b.endTime AND :endTime > b.startTime)
          )
    """)
    boolean existsOverlappingBooking(
            @Param("resourceId") UUID resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

}
