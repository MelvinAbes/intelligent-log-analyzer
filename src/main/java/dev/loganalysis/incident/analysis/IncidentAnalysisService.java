package dev.loganalysis.incident.analysis;

import dev.loganalysis.event.domain.LogEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IncidentAnalysisService {

    private final List<IncidentDetector> detectors;
    private final IncidentReconciler reconciler;
    private final Clock clock;
    private final Counter incidentCounter;

    public IncidentAnalysisService(
            List<IncidentDetector> detectors,
            IncidentReconciler reconciler,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.detectors = List.copyOf(detectors);
        this.reconciler = reconciler;
        this.clock = clock;
        incidentCounter =
                Counter.builder("log_analysis.incidents.detected")
                        .description("Incident candidates reconciled")
                        .register(meterRegistry);
    }

    public List<UUID> analyze(List<LogEvent> events) {
        if (events.isEmpty()) {
            return List.of();
        }
        DetectionContext context = new DetectionContext(events, clock.instant());
        List<UUID> incidentIds =
                detectors.stream()
                        .flatMap(detector -> detector.detect(context).stream())
                        .map(reconciler::reconcile)
                        .distinct()
                        .toList();
        incidentCounter.increment(incidentIds.size());
        return incidentIds;
    }
}
