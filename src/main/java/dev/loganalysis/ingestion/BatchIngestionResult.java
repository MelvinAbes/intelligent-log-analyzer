package dev.loganalysis.ingestion;

import dev.loganalysis.event.domain.LogEvent;
import java.util.List;

public record BatchIngestionResult(List<LogEvent> events, List<LineRejection> rejections) {

    public BatchIngestionResult {
        events = List.copyOf(events);
        rejections = List.copyOf(rejections);
    }

    public int acceptedCount() {
        return events.size();
    }

    public int rejectedCount() {
        return rejections.size();
    }
}
