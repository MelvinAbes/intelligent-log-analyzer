package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.IncidentEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IncidentRepository extends JpaRepository<IncidentEntity, UUID> {

    Optional<IncidentEntity> findByRuleCodeAndGroupingKeyAndWindowStart(
            String ruleCode, String groupingKey, Instant windowStart);
}
