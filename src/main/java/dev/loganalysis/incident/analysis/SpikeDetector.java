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
public class SpikeDetector implements IncidentDetector {

    public static final String RULE_CODE = "event_spike";
    private static final Duration BUCKET = Duration.ofMinutes(1);
    private static final int BASELINE_BUCKETS = 5;

    private final LogEventRepository eventRepository;
    private final IncidentSeverityPolicy severityPolicy;
    private final AnalysisProperties properties;

    public SpikeDetector(
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
        Map<String, LogEvent> groups = new LinkedHashMap<>();
        context.newEvents()
                .forEach(
                        event -> {
                            Instant bucketStart =
                                    DetectionWindows.floor(event.occurredAt(), BUCKET);
                            groups.put(event.service() + ":" + bucketStart, event);
                        });
        return groups.values().stream()
                .map(this::candidate)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private IncidentCandidate candidate(LogEvent trigger) {
        Instant bucketStart = DetectionWindows.floor(trigger.occurredAt(), BUCKET);
        Instant baselineStart = bucketStart.minus(BUCKET.multipliedBy(BASELINE_BUCKETS));
        List<LogEvent> events =
                eventRepository
                        .findByServiceAndOccurredAtBetweenOrderByOccurredAtAsc(
                                trigger.service(), baselineStart, bucketStart.plus(BUCKET))
                        .stream()
                        .map(LogEventEntity::toDomain)
                        .toList();
        List<LogEvent> current =
                events.stream().filter(event -> !event.occurredAt().isBefore(bucketStart)).toList();
        int minimum = properties.detection().spikeMinimumCount();
        if (current.size() < minimum) {
            return null;
        }
        List<Integer> baselineCounts = baselineCounts(events, baselineStart, bucketStart);
        double median = median(baselineCounts);
        double required = median == 0 ? minimum : median * properties.detection().spikeMultiplier();
        if (current.size() < required) {
            return null;
        }
        Map<String, Object> evidence =
                Map.of(
                        "service",
                        trigger.service(),
                        "observed_count",
                        current.size(),
                        "minimum_count",
                        minimum,
                        "baseline_median",
                        median,
                        "multiplier",
                        properties.detection().spikeMultiplier(),
                        "bucket_seconds",
                        BUCKET.toSeconds());
        return new IncidentCandidate(
                ruleCode(),
                trigger.service(),
                severityPolicy.spike(current.size(), minimum),
                bucketStart,
                current.getFirst().occurredAt(),
                current.getLast().occurredAt(),
                current.stream().map(LogEvent::id).toList(),
                evidence);
    }

    private static List<Integer> baselineCounts(
            List<LogEvent> events, Instant baselineStart, Instant bucketStart) {
        List<Integer> counts = new ArrayList<>(BASELINE_BUCKETS);
        for (int index = 0; index < BASELINE_BUCKETS; index++) {
            Instant start = baselineStart.plus(BUCKET.multipliedBy(index));
            Instant end = start.plus(BUCKET);
            int count =
                    (int)
                            events.stream()
                                    .filter(event -> !event.occurredAt().isBefore(start))
                                    .filter(event -> event.occurredAt().isBefore(end))
                                    .count();
            counts.add(count);
        }
        return counts;
    }

    private static double median(List<Integer> values) {
        List<Integer> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }
}
