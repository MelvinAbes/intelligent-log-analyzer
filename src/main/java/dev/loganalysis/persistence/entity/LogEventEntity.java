package dev.loganalysis.persistence.entity;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "log_events")
public class LogEventEntity {

    @Id private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_id")
    private LogImportEntity logImport;

    @Column(name = "line_number")
    private Integer lineNumber;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @Column(nullable = false, length = 128)
    private String source;

    @Column(nullable = false, length = 128)
    private String service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EventSeverity severity;

    @Column(name = "event_type", length = 128)
    private String eventType;

    @Column(nullable = false, length = 4096)
    private String message;

    @Column(name = "trace_id", length = 128)
    private String traceId;

    @Column(name = "subject_id", length = 128)
    private String subjectId;

    @Column(name = "external_id", length = 128)
    private String externalId;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> attributes = new LinkedHashMap<>();

    @Column(name = "raw_line", length = 16_384)
    private String sanitizedRawLine;

    protected LogEventEntity() {}

    public LogEventEntity(LogEvent event, LogImportEntity logImport, Integer lineNumber) {
        id = event.id();
        this.logImport = logImport;
        this.lineNumber = lineNumber;
        occurredAt = event.occurredAt();
        receivedAt = event.receivedAt();
        source = event.source();
        service = event.service();
        severity = event.severity();
        eventType = event.eventType();
        message = event.message();
        traceId = event.traceId();
        subjectId = event.subjectId();
        externalId = event.externalId();
        fingerprint = event.fingerprint();
        attributes = new LinkedHashMap<>(event.attributes());
        sanitizedRawLine = event.sanitizedRawLine();
    }

    public LogEvent toDomain() {
        return new LogEvent(
                id,
                logImport == null ? null : logImport.getId(),
                lineNumber,
                occurredAt,
                receivedAt,
                source,
                service,
                severity,
                eventType,
                message,
                traceId,
                subjectId,
                externalId,
                fingerprint,
                attributes,
                sanitizedRawLine);
    }

    public UUID getId() {
        return id;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getSource() {
        return source;
    }

    public String getService() {
        return service;
    }

    public EventSeverity getSeverity() {
        return severity;
    }

    public String getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getTraceId() {
        return traceId;
    }

    public String getSubjectId() {
        return subjectId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public Map<String, String> getAttributes() {
        return Map.copyOf(attributes);
    }
}
