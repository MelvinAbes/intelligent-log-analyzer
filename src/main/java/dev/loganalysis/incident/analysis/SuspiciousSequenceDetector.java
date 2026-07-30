package dev.loganalysis.incident.analysis;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.LogEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class SuspiciousSequenceDetector implements IncidentDetector {

    public static final String RULE_CODE = "suspicious_privilege_sequence";
    private static final int REQUIRED_FAILURES = 3;

    private final LogEventRepository eventRepository;
    private final IncidentSeverityPolicy severityPolicy;
    private final AnalysisProperties properties;

    public SuspiciousSequenceDetector(
            LogEventRepository eventRepository,
            IncidentSeverityPolicy severityPolicy,
            AnalysisProperties properties) {
        this.eventRepository = eventRepository;
        this.severityPolicy = severityPolicy;
        this.properties = properties;
    }

    @Override
    public String ruleCode() {
        return RULE_CODE;
    }

    @Override
    public List<IncidentCandidate> detect(DetectionContext context) {
        Map<String, LogEvent> privilegeChanges = new LinkedHashMap<>();
        context.newEvents().stream()
                .filter(event -> event.subjectId() != null)
                .filter(event -> "privilege.changed".equals(event.eventType()))
                .forEach(event -> privilegeChanges.put(event.subjectId(), event));
        return privilegeChanges.values().stream()
                .map(this::candidate)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private IncidentCandidate candidate(LogEvent privilegeChange) {
        Duration window = properties.detection().sequenceWindow();
        Instant windowStart = privilegeChange.occurredAt().minus(window);
        List<LogEvent> events =
                eventRepository
                        .findBySubjectIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                                privilegeChange.subjectId(),
                                windowStart,
                                privilegeChange.occurredAt())
                        .stream()
                        .map(LogEventEntity::toDomain)
                        .toList();
        List<LogEvent> failures = new ArrayList<>();
        LogEvent successfulAuthentication = null;
        for (LogEvent event : events) {
            if ("authentication.failed".equals(event.eventType())
                    && successfulAuthentication == null) {
                failures.add(event);
            }
            if ("authentication.succeeded".equals(event.eventType())
                    && failures.size() >= REQUIRED_FAILURES) {
                successfulAuthentication = event;
            }
        }
        if (successfulAuthentication == null) {
            return null;
        }
        List<LogEvent> evidenceEvents = new ArrayList<>(failures);
        evidenceEvents.add(successfulAuthentication);
        evidenceEvents.add(privilegeChange);
        Instant incidentWindow = DetectionWindows.floor(privilegeChange.occurredAt(), window);
        Map<String, Object> evidence =
                Map.of(
                        "subject_id",
                        privilegeChange.subjectId(),
                        "failed_authentications",
                        failures.size(),
                        "required_failures",
                        REQUIRED_FAILURES,
                        "successful_authentication_at",
                        successfulAuthentication.occurredAt().toString(),
                        "privilege_change_at",
                        privilegeChange.occurredAt().toString(),
                        "window_seconds",
                        window.toSeconds());
        return new IncidentCandidate(
                ruleCode(),
                privilegeChange.subjectId(),
                severityPolicy.suspiciousSequence(),
                incidentWindow,
                evidenceEvents.getFirst().occurredAt(),
                privilegeChange.occurredAt(),
                evidenceEvents.stream().map(LogEvent::id).distinct().toList(),
                evidence);
    }
}
