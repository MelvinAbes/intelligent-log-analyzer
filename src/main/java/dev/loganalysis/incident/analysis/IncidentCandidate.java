package dev.loganalysis.incident.analysis;

import dev.loganalysis.incident.domain.IncidentSeverity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record IncidentCandidate(
        String ruleCode,
        String groupingKey,
        IncidentSeverity severity,
        Instant windowStart,
        Instant startedAt,
        Instant endedAt,
        List<UUID> eventIds,
        Map<String, Object> evidence) {

    public IncidentCandidate {
        if (ruleCode == null || ruleCode.isBlank()) {
            throw new IllegalArgumentException("rule code must not be blank");
        }
        if (groupingKey == null || groupingKey.isBlank()) {
            throw new IllegalArgumentException("grouping key must not be blank");
        }
        if (severity == null || windowStart == null || startedAt == null || endedAt == null) {
            throw new IllegalArgumentException("severity and timestamps are required");
        }
        if (startedAt.isAfter(endedAt)) {
            throw new IllegalArgumentException("incident start must not follow its end");
        }
        eventIds = List.copyOf(eventIds);
        if (eventIds.isEmpty()) {
            throw new IllegalArgumentException("incident must contain evidence events");
        }
        evidence = Map.copyOf(evidence);
    }
}
