package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.IncidentEventEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, UUID> {

    boolean existsByIncidentIdAndEventId(UUID incidentId, UUID eventId);
}
