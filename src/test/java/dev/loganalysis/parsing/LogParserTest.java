package dev.loganalysis.parsing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class LogParserTest {

    private LogParserRegistry registry;

    @BeforeEach
    void setUp() {
        registry =
                new LogParserRegistry(
                        List.of(
                                new JsonLinesLogParser(JsonMapper.builder().build()),
                                new SyslogLogParser(),
                                new ApplicationLogParser(),
                                new AccessLogParser()));
    }

    @Test
    void parsesJsonLogAndScalarAttributes() {
        ParsedLogLine parsed =
                registry.parse(
                        """
            {"timestamp":"2026-07-30T10:00:00Z","level":"error","service":"payments",\
            "source":"api-1","message":"Charge failed","event_type":"payment.failed",\
            "trace_id":"trace-1","attributes":{"region":"eu-central","attempt":3}}
            """,
                        LogFormat.AUTO,
                        new ParseHints(null, null));

        assertThat(parsed.format()).isEqualTo(LogFormat.JSON_LINES);
        assertThat(parsed.severity()).isEqualTo(EventSeverity.ERROR);
        assertThat(parsed.attributes())
                .containsEntry("region", "eu-central")
                .containsEntry("attempt", "3");
    }

    @Test
    void parsesRfc5424SyslogPriorityAndStructuredFields() {
        ParsedLogLine parsed =
                registry.parse(
                        "<11>1 2026-07-30T10:01:00Z edge-7 device-agent 81 AUTH "
                                + "[context trace_id=\"trace-9\" subject_id=\"device-9\"] Authentication failed",
                        LogFormat.SYSLOG,
                        new ParseHints(null, null));

        assertThat(parsed.severity()).isEqualTo(EventSeverity.ERROR);
        assertThat(parsed.source()).isEqualTo("edge-7");
        assertThat(parsed.service()).isEqualTo("device-agent");
        assertThat(parsed.eventType()).isEqualTo("AUTH");
        assertThat(parsed.traceId()).isEqualTo("trace-9");
    }

    @Test
    void parsesCommonApplicationLogWithHintsAndAttributes() {
        ParsedLogLine parsed =
                registry.parse(
                        "2026-07-30T10:02:00Z WARN event_type=cache.miss trace_id=t-7 key=profile-17",
                        LogFormat.APPLICATION,
                        new ParseHints("worker-2", "profile-service"));

        assertThat(parsed.source()).isEqualTo("worker-2");
        assertThat(parsed.service()).isEqualTo("profile-service");
        assertThat(parsed.eventType()).isEqualTo("cache.miss");
        assertThat(parsed.traceId()).isEqualTo("t-7");
    }

    @Test
    void parsesCombinedAccessLogAndMapsServerErrors() {
        ParsedLogLine parsed =
                registry.parse(
                        "203.0.113.17 - - [30/Jul/2026:10:03:00 +0000] "
                                + "\"GET /health HTTP/1.1\" 503 91 \"-\" \"synthetic-probe/1.0\"",
                        LogFormat.ACCESS,
                        new ParseHints(null, "edge-gateway"));

        assertThat(parsed.severity()).isEqualTo(EventSeverity.ERROR);
        assertThat(parsed.eventType()).isEqualTo("http.503");
        assertThat(parsed.attributes()).containsEntry("path", "/health");
    }

    @Test
    void factoryRedactsSecretsAndCreatesStableFingerprint() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-30T10:05:00Z"), ZoneOffset.UTC);
        LogEventFactory factory =
                new LogEventFactory(
                        registry, new SensitiveDataRedactor(), new MessageFingerprint(), clock);
        String first =
                "2026-07-30T10:02:00Z ERROR [auth] [api-1] "
                        + "Login failed user_id=17 token=abc123 request=81";
        String second =
                "2026-07-30T10:02:01Z ERROR [auth] [api-1] "
                        + "Login failed user_id=24 token=def456 request=92";

        var firstEvent =
                factory.create(
                        first, LogFormat.APPLICATION, new ParseHints(null, null), null, null);
        var secondEvent =
                factory.create(
                        second, LogFormat.APPLICATION, new ParseHints(null, null), null, null);

        assertThat(firstEvent.message()).contains("token=[REDACTED]").doesNotContain("abc123");
        assertThat(firstEvent.attributes()).containsEntry("token", "[REDACTED]");
        assertThat(firstEvent.sanitizedRawLine()).doesNotContain("abc123");
        assertThat(firstEvent.fingerprint()).isEqualTo(secondEvent.fingerprint());
    }

    @Test
    void rejectsUnknownAndFutureDatedLines() {
        assertThatThrownBy(
                        () ->
                                registry.parse(
                                        "not a supported line",
                                        LogFormat.AUTO,
                                        new ParseHints(null, null)))
                .isInstanceOf(LogParsingException.class)
                .extracting(error -> ((LogParsingException) error).code())
                .isEqualTo("unknown_format");

        Clock clock = Clock.fixed(Instant.parse("2026-07-30T10:00:00Z"), ZoneOffset.UTC);
        LogEventFactory factory =
                new LogEventFactory(
                        registry, new SensitiveDataRedactor(), new MessageFingerprint(), clock);
        assertThatThrownBy(
                        () ->
                                factory.create(
                                        "2026-07-30T10:06:00Z INFO event_type=clock.test",
                                        LogFormat.APPLICATION,
                                        new ParseHints("source", "service"),
                                        null,
                                        null))
                .isInstanceOf(LogParsingException.class)
                .extracting(error -> ((LogParsingException) error).code())
                .isEqualTo("future_timestamp");
    }
}
