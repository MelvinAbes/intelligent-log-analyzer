package dev.loganalysis.incident.analysis;

import dev.loganalysis.event.domain.LogEvent;
import java.time.Instant;
import java.util.List;

public record DetectionContext(List<LogEvent> newEvents, Instant analyzedAt) {

    public DetectionContext {
        newEvents = List.copyOf(newEvents);
        if (analyzedAt == null) {
            throw new IllegalArgumentException("analysis time is required");
        }
    }
}
