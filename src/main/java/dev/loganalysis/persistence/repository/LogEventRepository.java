package dev.loganalysis.persistence.repository;

import dev.loganalysis.persistence.entity.LogEventEntity;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogEventRepository extends JpaRepository<LogEventEntity, UUID> {

    boolean existsBySourceAndExternalId(String source, String externalId);

    @Query(
            """
      select e from LogEventEntity e
      where e.service = :service
        and e.fingerprint = :fingerprint
        and e.occurredAt >= :fromTime
        and e.occurredAt < :toTime
      order by e.occurredAt
      """)
    List<LogEventEntity> findByServiceAndFingerprintAndOccurredAtBetweenOrderByOccurredAtAsc(
            @Param("service") String service,
            @Param("fingerprint") String fingerprint,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to);

    @Query(
            """
      select e from LogEventEntity e
      where e.service = :service
        and e.occurredAt >= :fromTime
        and e.occurredAt < :toTime
      order by e.occurredAt
      """)
    List<LogEventEntity> findByServiceAndOccurredAtBetweenOrderByOccurredAtAsc(
            @Param("service") String service,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to);

    @Query(
            """
      select e from LogEventEntity e
      where e.subjectId = :subjectId
        and e.occurredAt >= :fromTime
        and e.occurredAt <= :toTime
      order by e.occurredAt
      """)
    List<LogEventEntity> findBySubjectIdAndOccurredAtBetweenOrderByOccurredAtAsc(
            @Param("subjectId") String subjectId,
            @Param("fromTime") Instant from,
            @Param("toTime") Instant to);

    @Query(
            value =
                    """
          SELECT *
          FROM log_events
          WHERE occurred_at >= :from_time
            AND occurred_at < :to_time
            AND (:source = '' OR source = :source)
            AND (:service = '' OR service = :service)
            AND (:severity = '' OR severity = :severity)
            AND (
              :query = ''
              OR search_vector @@ websearch_to_tsquery('simple'::regconfig, :query)
            )
          ORDER BY occurred_at DESC, id
          """,
            countQuery =
                    """
          SELECT count(*)
          FROM log_events
          WHERE occurred_at >= :from_time
            AND occurred_at < :to_time
            AND (:source = '' OR source = :source)
            AND (:service = '' OR service = :service)
            AND (:severity = '' OR severity = :severity)
            AND (
              :query = ''
              OR search_vector @@ websearch_to_tsquery('simple'::regconfig, :query)
            )
          """,
            nativeQuery = true)
    Page<LogEventEntity> search(
            @Param("from_time") Instant from,
            @Param("to_time") Instant to,
            @Param("source") String source,
            @Param("service") String service,
            @Param("severity") String severity,
            @Param("query") String query,
            Pageable pageable);
}
