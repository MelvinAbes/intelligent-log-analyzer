package dev.loganalysis.incident.analysis;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.incident.domain.IncidentSeverity;
import java.util.Collection;
import org.springframework.stereotype.Component;

@Component
public class IncidentSeverityPolicy {

    public IncidentSeverity repeatedError(
            Collection<EventSeverity> eventSeverities, int observedCount, int threshold) {
        boolean containsFatal =
                eventSeverities.stream().anyMatch(severity -> severity == EventSeverity.FATAL);
        if (containsFatal || observedCount >= threshold * 2) {
            return IncidentSeverity.CRITICAL;
        }
        return IncidentSeverity.HIGH;
    }

    public IncidentSeverity spike(int observedCount, int minimumCount) {
        return observedCount >= minimumCount * 2 ? IncidentSeverity.HIGH : IncidentSeverity.MEDIUM;
    }

    public IncidentSeverity suspiciousSequence() {
        return IncidentSeverity.CRITICAL;
    }
}
