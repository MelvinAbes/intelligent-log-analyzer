package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.time.Instant;
import java.util.Map;

public record ParsedLogLine(
        LogFormat format,
        Instant occurredAt,
        String source,
        String service,
        EventSeverity severity,
        String eventType,
        String message,
        String traceId,
        String subjectId,
        String externalId,
        Map<String, String> attributes) {

    public ParsedLogLine {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
