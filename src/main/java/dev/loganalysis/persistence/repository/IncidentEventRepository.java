package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.IncidentEventEntity;
import dev.loganalysis.persistence.entity.LogEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentEventRepository extends JpaRepository<IncidentEventEntity, UUID> {

    boolean existsByIncidentIdAndEventId(UUID incidentId, UUID eventId);

    @Query(
            """
      select link.event from IncidentEventEntity link
      where link.incident.id = :incidentId
      order by link.event.occurredAt, link.event.id
      """)
    List<LogEventEntity> findTimeline(@Param("incidentId") UUID incidentId);
}
