package dev.loganalysis.incident.analysis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.LogEventRepository;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class IncidentDetectorTest {

    private LogEventRepository repository;
    private IncidentSeverityPolicy severityPolicy;
    private AnalysisProperties properties;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(LogEventRepository.class);
        severityPolicy = new IncidentSeverityPolicy();
        properties =
                new AnalysisProperties(
                        new AnalysisProperties.ImportSettings(
                                Path.of("data"), 100, 4096, 50, Duration.ofMinutes(2)),
                        new AnalysisProperties.DetectionSettings(
                                5, Duration.ofMinutes(5), 10, 3.0, Duration.ofMinutes(10)),
                        new AnalysisProperties.SummarySettings(
                                "disabled", "http://127.0.0.1", "local", ""));
    }

    @Test
    void repeatedErrorsIncludeThresholdEvidence() {
        LogEvent trigger = event(0, "payments", EventSeverity.ERROR, "payment.failed", null);
        List<LogEventEntity> stored =
                java.util.stream.IntStream.range(0, 5)
                        .mapToObj(
                                index ->
                                        new LogEventEntity(
                                                event(
                                                        index,
                                                        "payments",
                                                        EventSeverity.ERROR,
                                                        "payment.failed",
                                                        null),
                                                null,
                                                null))
                        .toList();
        when(repository.findByServiceAndFingerprintAndOccurredAtBetweenOrderByOccurredAtAsc(
                        eq("payments"), eq(trigger.fingerprint()), any(), any()))
                .thenReturn(stored);

        var candidates =
                new RepeatedErrorDetector(repository, severityPolicy, properties)
                        .detect(new DetectionContext(List.of(trigger), trigger.receivedAt()));

        assertThat(candidates).singleElement();
        assertThat(candidates.getFirst().severity()).isEqualTo(IncidentSeverity.HIGH);
        assertThat(candidates.getFirst().evidence()).containsEntry("threshold", 5);
    }

    @Test
    void spikeUsesMedianOfPreviousBuckets() {
        LogEvent trigger = event(360, "gateway", EventSeverity.INFO, "request.completed", null);
        List<LogEventEntity> stored =
                java.util.stream.IntStream.range(0, 12)
                        .mapToObj(
                                index ->
                                        new LogEventEntity(
                                                event(
                                                        360 + index,
                                                        "gateway",
                                                        EventSeverity.INFO,
                                                        "request.completed",
                                                        null),
                                                null,
                                                null))
                        .toList();
        when(repository.findByServiceAndOccurredAtBetweenOrderByOccurredAtAsc(
                        eq("gateway"), any(), any()))
                .thenReturn(stored);

        var candidates =
                new SpikeDetector(repository, severityPolicy, properties)
                        .detect(new DetectionContext(List.of(trigger), trigger.receivedAt()));

        assertThat(candidates).singleElement();
        assertThat(candidates.getFirst().severity()).isEqualTo(IncidentSeverity.MEDIUM);
        assertThat(candidates.getFirst().evidence()).containsEntry("baseline_median", 0.0);
    }

    @Test
    void suspiciousSequenceRequiresFailuresSuccessAndPrivilegeChange() {
        String subject = "operator-17";
        List<LogEvent> sequence =
                List.of(
                        event(0, "identity", EventSeverity.WARN, "authentication.failed", subject),
                        event(30, "identity", EventSeverity.WARN, "authentication.failed", subject),
                        event(60, "identity", EventSeverity.WARN, "authentication.failed", subject),
                        event(
                                90,
                                "identity",
                                EventSeverity.INFO,
                                "authentication.succeeded",
                                subject),
                        event(120, "identity", EventSeverity.WARN, "privilege.changed", subject));
        when(repository.findBySubjectIdAndOccurredAtBetweenOrderByOccurredAtAsc(
                        eq(subject), any(), any()))
                .thenReturn(
                        sequence.stream()
                                .map(event -> new LogEventEntity(event, null, null))
                                .toList());

        var candidates =
                new SuspiciousSequenceDetector(repository, severityPolicy, properties)
                        .detect(
                                new DetectionContext(
                                        List.of(sequence.getLast()),
                                        sequence.getLast().receivedAt()));

        assertThat(candidates).singleElement();
        assertThat(candidates.getFirst().severity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(candidates.getFirst().eventIds()).hasSize(5);
    }

    private static LogEvent event(
            int offsetSeconds,
            String service,
            EventSeverity severity,
            String eventType,
            String subjectId) {
        Instant occurredAt = Instant.parse("2026-07-30T10:00:00Z").plusSeconds(offsetSeconds);
        return new LogEvent(
                UUID.randomUUID(),
                null,
                null,
                occurredAt,
                occurredAt.plusSeconds(1),
                "source-1",
                service,
                severity,
                eventType,
                eventType + " request=" + offsetSeconds,
                null,
                subjectId,
                null,
                "fingerprint-1",
                Map.of(),
                null);
    }
}
