package dev.loganalysis.event.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record LogEvent(
        UUID id,
        UUID importId,
        Integer lineNumber,
        Instant occurredAt,
        Instant receivedAt,
        String source,
        String service,
        EventSeverity severity,
        String eventType,
        String message,
        String traceId,
        String subjectId,
        String externalId,
        String fingerprint,
        Map<String, String> attributes,
        String sanitizedRawLine) {

    public LogEvent {
        id = require(id, "id");
        occurredAt = require(occurredAt, "occurredAt");
        receivedAt = require(receivedAt, "receivedAt");
        severity = require(severity, "severity");
        source = requireText(source, "source");
        service = requireText(service, "service");
        message = requireText(message, "message");
        fingerprint = requireText(fingerprint, "fingerprint");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static <T> T require(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
