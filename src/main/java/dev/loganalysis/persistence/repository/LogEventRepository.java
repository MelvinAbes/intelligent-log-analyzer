package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.LogEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogEventRepository extends JpaRepository<LogEventEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    List<LogEventEntity> findByServiceAndFingerprintAndOccurredAtBetweenOrderByOccurredAtAsc(
            String service, String fingerprint, Instant from, Instant to);

    List<LogEventEntity> findByServiceAndOccurredAtBetweenOrderByOccurredAtAsc(
            String service, Instant from, Instant to);

    List<LogEventEntity> findBySubjectIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            String subjectId, Instant from, Instant to);
}
