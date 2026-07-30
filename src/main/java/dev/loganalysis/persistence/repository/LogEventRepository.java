package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.LogEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEventRepository extends JpaRepository<LogEventEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);
}
