package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;

final class ParserSupport {

    private ParserSupport() {}

    static Instant parseInstant(String value, LogFormat format) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException error) {
            throw new LogParsingException(
                    "invalid_timestamp",
                    format,
                    "Log timestamp must be an ISO-8601 instant.",
                    error);
        }
    }

    static EventSeverity parseSeverity(String value, LogFormat format) {
        try {
            return EventSeverity.parse(value);
        } catch (IllegalArgumentException error) {
            throw new LogParsingException(
                    "invalid_severity", format, "Log severity is not recognized.", error);
        }
    }

    static String firstPresent(String primary, String fallback, String field, LogFormat format) {
        String selected = primary == null || primary.isBlank() ? fallback : primary;
        if (selected == null || selected.isBlank()) {
            throw new LogParsingException(
                    "missing_" + field, format, "Log " + field + " is required.");
        }
        return selected.strip();
    }
}
