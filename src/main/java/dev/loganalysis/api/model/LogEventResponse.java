package dev.loganalysis.api.model;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LogEventResponse(
        UUID id,
        Instant occurredAt,
        String source,
        String service,
        EventSeverity severity,
        String eventType,
        String message,
        String traceId,
        String fingerprint,
        Map<String, String> attributes) {

    public static LogEventResponse from(LogEvent event) {
        return new LogEventResponse(
                event.id(),
                event.occurredAt(),
                event.source(),
                event.service(),
                event.severity(),
                event.eventType(),
                event.message(),
                event.traceId(),
                event.fingerprint(),
                event.attributes());
    }
}
