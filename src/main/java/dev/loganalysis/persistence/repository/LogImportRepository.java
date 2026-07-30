package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.LogImportEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogImportRepository extends JpaRepository<LogImportEntity, UUID> {

    @Query(
            value =
                    """
          SELECT *
          FROM log_imports
          WHERE status = 'QUEUED'
             OR (status = 'PROCESSING' AND lease_until < :now)
          ORDER BY created_at
          LIMIT 1
          FOR UPDATE SKIP LOCKED
          """,
            nativeQuery = true)
    Optional<LogImportEntity> findNextClaimable(@Param("now") Instant now);
}
