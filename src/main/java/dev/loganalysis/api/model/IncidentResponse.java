package dev.loganalysis.api.model;

import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.incident.domain.IncidentStatus;
import dev.loganalysis.persistence.entity.IncidentEntity;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String ruleCode,
        String groupingKey,
        IncidentSeverity severity,
        IncidentStatus status,
        String title,
        String summary,
        Instant startedAt,
        Instant endedAt,
        Instant detectedAt,
        Instant updatedAt,
        int eventCount,
        Map<String, Object> evidence) {

    public static IncidentResponse from(IncidentEntity incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getRuleCode(),
                incident.getGroupingKey(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getTitle(),
                incident.getSummary(),
                incident.getStartedAt(),
                incident.getEndedAt(),
                incident.getDetectedAt(),
                incident.getUpdatedAt(),
                incident.getEventCount(),
                incident.getEvidence());
    }
}
