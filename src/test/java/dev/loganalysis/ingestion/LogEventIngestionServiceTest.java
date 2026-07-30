package dev.loganalysis.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.incident.analysis.IncidentAnalysisService;
import dev.loganalysis.parsing.AccessLogParser;
import dev.loganalysis.parsing.ApplicationLogParser;
import dev.loganalysis.parsing.JsonLinesLogParser;
import dev.loganalysis.parsing.LogEventFactory;
import dev.loganalysis.parsing.LogParserRegistry;
import dev.loganalysis.parsing.MessageFingerprint;
import dev.loganalysis.parsing.ParseHints;
import dev.loganalysis.parsing.SensitiveDataRedactor;
import dev.loganalysis.parsing.SyslogLogParser;
import dev.loganalysis.persistence.repository.LogEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.json.JsonMapper;

class LogEventIngestionServiceTest {

    @Test
    void returnsAcceptedEventsAndSafePerLineRejections() {
        LogEventRepository repository = Mockito.mock(LogEventRepository.class);
        IncidentAnalysisService analysisService = Mockito.mock(IncidentAnalysisService.class);
        LogParserRegistry registry =
                new LogParserRegistry(
                        List.of(
                                new JsonLinesLogParser(JsonMapper.builder().build()),
                                new SyslogLogParser(),
                                new AccessLogParser(),
                                new ApplicationLogParser()));
        LogEventFactory factory =
                new LogEventFactory(
                        registry,
                        new SensitiveDataRedactor(),
                        new MessageFingerprint(),
                        Clock.fixed(Instant.parse("2026-07-30T10:05:00Z"), ZoneOffset.UTC));
        LogEventIngestionService service =
                new LogEventIngestionService(
                        factory, repository, analysisService, new SimpleMeterRegistry());

        BatchIngestionResult result =
                service.ingest(
                        List.of(
                                new NumberedLogLine(
                                        1,
                                        "2026-07-30T10:00:00Z ERROR event_type=payment.failed order=17"),
                                new NumberedLogLine(2, "invalid")),
                        LogFormat.AUTO,
                        new ParseHints("api-1", "payments"),
                        null);

        assertThat(result.acceptedCount()).isEqualTo(1);
        assertThat(result.rejectedCount()).isEqualTo(1);
        assertThat(result.rejections().getFirst().code()).isEqualTo("unknown_format");
        Mockito.verify(repository)
                .saveAllAndFlush(Mockito.argThat(values -> values.iterator().hasNext()));
        Mockito.verify(analysisService).analyze(Mockito.argThat(values -> values.size() == 1));
    }
}
