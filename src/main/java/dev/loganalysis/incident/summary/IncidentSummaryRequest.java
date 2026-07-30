package dev.loganalysis.incident.summary;

import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.persistence.entity.IncidentEntity;
import java.util.List;

public record IncidentSummaryRequest(IncidentEntity incident, List<LogEvent> timeline) {

    public IncidentSummaryRequest {
        timeline = List.copyOf(timeline);
    }
}
