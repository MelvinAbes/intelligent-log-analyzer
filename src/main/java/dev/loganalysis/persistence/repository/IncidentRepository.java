package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.IncidentEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Optional<IncidentEntity> findByRuleCodeAndGroupingKeyAndWindowStart(
            String ruleCode, String groupingKey, Instant windowStart);

    @Query(
            """
      select i from IncidentEntity i
      where i.startedAt < :toTime
        and i.endedAt >= :fromTime
        and (:status is null or i.status = :status)
        and (:severity is null or i.severity = :severity)
        and (:ruleCode = '' or i.ruleCode = :ruleCode)
      order by i.startedAt desc, i.id
      """)
    Page<IncidentEntity> search(
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to,
            @Param("status") dev.loganalysis.incident.domain.IncidentStatus status,
            @Param("severity") dev.loganalysis.incident.domain.IncidentSeverity severity,
            @Param("ruleCode") String ruleCode,
            Pageable pageable);
}
