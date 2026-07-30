package dev.loganalysis.incident.analysis;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.LogEventRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RepeatedErrorDetector implements IncidentDetector {

    public static final String RULE_CODE = "repeated_error";

    private final LogEventRepository eventRepository;
    private final IncidentSeverityPolicy severityPolicy;
    private final AnalysisProperties properties;

    public RepeatedErrorDetector(
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
        Duration window = properties.detection().repeatedErrorWindow();
        Map<String, LogEvent> groups = new LinkedHashMap<>();
        context.newEvents().stream()
                .filter(event -> event.severity().weight() >= EventSeverity.ERROR.weight())
                .forEach(
                        event -> {
                            Instant windowStart =
                                    DetectionWindows.floor(event.occurredAt(), window);
                            String key =
                                    event.service() + ":" + event.fingerprint() + ":" + windowStart;
                            groups.put(key, event);
                        });

        return groups.values().stream()
                .map(event -> candidate(event, window))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private IncidentCandidate candidate(LogEvent trigger, Duration window) {
        Instant windowStart = DetectionWindows.floor(trigger.occurredAt(), window);
        List<LogEvent> events =
                eventRepository
                        .findByServiceAndFingerprintAndOccurredAtBetweenOrderByOccurredAtAsc(
                                trigger.service(),
                                trigger.fingerprint(),
                                windowStart,
                                windowStart.plus(window))
                        .stream()
                        .map(LogEventEntity::toDomain)
                        .toList();
        int threshold = properties.detection().repeatedErrorThreshold();
        if (events.size() < threshold) {
            return null;
        }
        Map<String, Object> evidence =
                Map.of(
                        "service",
                        trigger.service(),
                        "fingerprint",
                        trigger.fingerprint(),
                        "observed_count",
                        events.size(),
                        "threshold",
                        threshold,
                        "window_seconds",
                        window.toSeconds());
        return new IncidentCandidate(
                ruleCode(),
                trigger.service() + ":" + trigger.fingerprint(),
                severityPolicy.repeatedError(
                        events.stream().map(LogEvent::severity).toList(), events.size(), threshold),
                windowStart,
                events.getFirst().occurredAt(),
                events.getLast().occurredAt(),
                events.stream().map(LogEvent::id).toList(),
                evidence);
    }
}
