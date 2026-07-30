package dev.loganalysis.statistics;

import dev.loganalysis.query.InvalidQueryException;
import java.sql.ResultSet;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogStatisticsService {

    private static final Duration MAXIMUM_RANGE = Duration.ofDays(31);

    private final NamedParameterJdbcTemplate jdbc;
    private final Clock clock;

    public LogStatisticsService(NamedParameterJdbcTemplate jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LogStatistics calculate(
            Instant requestedFrom, Instant requestedTo, StatisticsBucket bucket) {
        Instant to = requestedTo == null ? clock.instant() : requestedTo;
        Instant from = requestedFrom == null ? to.minus(Duration.ofHours(24)) : requestedFrom;
        if (!from.isBefore(to)) {
            throw new InvalidQueryException("Statistics start must be before its end.");
        }
        if (Duration.between(from, to).compareTo(MAXIMUM_RANGE) > 0) {
            throw new InvalidQueryException("Statistics range must not exceed 31 days.");
        }
        Map<String, Object> parameters =
                Map.of(
                        "from_time",
                        OffsetDateTime.ofInstant(from, ZoneOffset.UTC),
                        "to_time",
                        OffsetDateTime.ofInstant(to, ZoneOffset.UTC));
        Long total =
                jdbc.queryForObject(
                        "SELECT count(*) FROM log_events WHERE occurred_at >= :from_time AND occurred_at < :to_time",
                        parameters,
                        Long.class);
        Long openIncidents =
                jdbc.queryForObject(
                        """
            SELECT count(*) FROM incidents
            WHERE status = 'OPEN' AND started_at < :to_time AND ended_at >= :from_time
            """,
                        parameters,
                        Long.class);
        List<NamedCount> bySeverity =
                jdbc.query(
                        """
            SELECT severity AS name, count(*) AS count
            FROM log_events
            WHERE occurred_at >= :from_time AND occurred_at < :to_time
            GROUP BY severity
            ORDER BY count DESC, severity
            """,
                        parameters,
                        namedCountMapper());
        List<NamedCount> byService =
                jdbc.query(
                        """
            SELECT service AS name, count(*) AS count
            FROM log_events
            WHERE occurred_at >= :from_time AND occurred_at < :to_time
            GROUP BY service
            ORDER BY count DESC, service
            LIMIT 10
            """,
                        parameters,
                        namedCountMapper());
        String timelineSql =
                """
        SELECT date_trunc('%s', occurred_at) AS bucket_start, count(*) AS count
        FROM log_events
        WHERE occurred_at >= :from_time AND occurred_at < :to_time
        GROUP BY bucket_start
        ORDER BY bucket_start
        """
                        .formatted(bucket.sqlUnit());
        List<TimeBucketCount> timeline =
                jdbc.query(
                        timelineSql,
                        parameters,
                        (result, rowNumber) ->
                                new TimeBucketCount(
                                        result.getObject("bucket_start", OffsetDateTime.class)
                                                .toInstant(),
                                        result.getLong("count")));
        return new LogStatistics(
                from,
                to,
                total == null ? 0 : total,
                openIncidents == null ? 0 : openIncidents,
                bySeverity,
                byService,
                timeline);
    }

    private static RowMapper<NamedCount> namedCountMapper() {
        return (ResultSet result, int rowNumber) ->
                new NamedCount(result.getString("name"), result.getLong("count"));
    }
}
