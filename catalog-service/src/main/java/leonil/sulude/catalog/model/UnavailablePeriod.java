package leonil.sulude.catalog.model;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import lombok.*;

import java.time.LocalDateTime;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class    UnavailablePeriod {

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;
}
