package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.event.domain.LogFormat;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class LogEventFactory {

    private static final Duration MAXIMUM_FUTURE_SKEW = Duration.ofMinutes(5);

    private final LogParserRegistry parsers;
    private final SensitiveDataRedactor redactor;
    private final MessageFingerprint fingerprints;
    private final Clock clock;

    public LogEventFactory(
            LogParserRegistry parsers,
            SensitiveDataRedactor redactor,
            MessageFingerprint fingerprints,
            Clock clock) {
        this.parsers = parsers;
        this.redactor = redactor;
        this.fingerprints = fingerprints;
        this.clock = clock;
    }

    public LogEvent create(
            String line, LogFormat format, ParseHints hints, UUID importId, Integer lineNumber) {
        ParsedLogLine parsed = parsers.parse(line, format, hints);
        Instant now = clock.instant();
        if (parsed.occurredAt().isAfter(now.plus(MAXIMUM_FUTURE_SKEW))) {
            throw new LogParsingException(
                    "future_timestamp",
                    parsed.format(),
                    "Log timestamp is more than five minutes in the future.");
        }
        String message = redactor.redact(parsed.message());
        return new LogEvent(
                UUID.randomUUID(),
                importId,
                lineNumber,
                parsed.occurredAt(),
                now,
                parsed.source(),
                parsed.service(),
                parsed.severity(),
                parsed.eventType(),
                message,
                parsed.traceId(),
                parsed.subjectId(),
                parsed.externalId(),
                fingerprints.create(message),
                redactor.redact(parsed.attributes()),
                redactor.redact(line));
    }
}
