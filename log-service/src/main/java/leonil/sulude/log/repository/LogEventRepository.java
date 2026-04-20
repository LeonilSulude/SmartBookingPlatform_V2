package leonil.sulude.log.repository;

import leonil.sulude.log.domain.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LogEventRepository extends JpaRepository<LogEvent, UUID> {
}
