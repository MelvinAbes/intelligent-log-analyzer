package dev.loganalysis.event.domain;

import java.time.Instant;
import java.util.Map;

public record LogEventDraft(
        Instant occurredAt,
        String source,
        String service,
        EventSeverity severity,
        String eventType,
        String message,
        String traceId,
        String subjectId,
        String externalId,
        Map<String, String> attributes,
        String sanitizedRawLine) {

    public LogEventDraft {
        occurredAt = requireValue(occurredAt, "occurredAt");
        source = requireText(source, "source", 128);
        service = requireText(service, "service", 128);
        severity = requireValue(severity, "severity");
        eventType = optionalText(eventType, 128);
        message = requireText(message, "message", 4096);
        traceId = optionalText(traceId, 128);
        subjectId = optionalText(subjectId, 128);
        externalId = optionalText(externalId, 128);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        sanitizedRawLine = optionalText(sanitizedRawLine, 16_384);
    }

    private static String requireText(String value, String field, int maximumLength) {
        String normalized = optionalText(value, maximumLength);
        if (normalized == null) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }

    private static String optionalText(String value, int maximumLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException("value exceeds " + maximumLength + " characters");
        }
        return normalized;
    }

    private static <T> T requireValue(T value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
        return value;
    }
}
