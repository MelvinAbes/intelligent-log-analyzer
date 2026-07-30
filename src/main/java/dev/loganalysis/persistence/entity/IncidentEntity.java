package dev.loganalysis.persistence.entity;

import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.incident.domain.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "incidents",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uq_incidents_rule_group_window",
                        columnNames = {"rule_code", "grouping_key", "window_start"}))
public class IncidentEntity {

    @Id private UUID id;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Column(name = "grouping_key", nullable = false, length = 255)
    private String groupingKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private IncidentStatus status;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 4096)
    private String summary;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at", nullable = false)
    private Instant endedAt;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "event_count", nullable = false)
    private int eventCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> evidence = new LinkedHashMap<>();

    @Version
    @Column(nullable = false)
    private long version;

    protected IncidentEntity() {}

    public IncidentEntity(
            String ruleCode,
            String groupingKey,
            IncidentSeverity severity,
            String title,
            String summary,
            Instant windowStart,
            Instant startedAt,
            Instant endedAt,
            Instant now,
            int eventCount,
            Map<String, Object> evidence) {
        id = UUID.randomUUID();
        this.ruleCode = ruleCode;
        this.groupingKey = groupingKey;
        this.severity = severity;
        status = IncidentStatus.OPEN;
        this.title = title;
        this.summary = summary;
        this.windowStart = windowStart;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        detectedAt = now;
        updatedAt = now;
        this.eventCount = eventCount;
        this.evidence = new LinkedHashMap<>(evidence);
    }

    public void update(
            IncidentSeverity severity,
            String title,
            String summary,
            Instant startedAt,
            Instant endedAt,
            Instant now,
            int eventCount,
            Map<String, Object> evidence) {
        this.severity = severity;
        this.title = title;
        this.summary = summary;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        updatedAt = now;
        this.eventCount = eventCount;
        this.evidence = new LinkedHashMap<>(evidence);
    }

    public void resolve(Instant now) {
        status = IncidentStatus.RESOLVED;
        updatedAt = now;
    }

    public void reopen(Instant now) {
        status = IncidentStatus.OPEN;
        updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getGroupingKey() {
        return groupingKey;
    }

    public IncidentSeverity getSeverity() {
        return severity;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getEventCount() {
        return eventCount;
    }

    public Map<String, Object> getEvidence() {
        return Map.copyOf(evidence);
    }
}
