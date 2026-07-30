package dev.loganalysis.ingestion;

import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.parsing.LogEventFactory;
import dev.loganalysis.parsing.LogParsingException;
import dev.loganalysis.parsing.ParseHints;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.entity.LogImportEntity;
import dev.loganalysis.persistence.repository.LogEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LogEventIngestionService {

    private final LogEventFactory eventFactory;
    private final LogEventRepository eventRepository;
    private final Counter acceptedCounter;
    private final Counter rejectedCounter;

    public LogEventIngestionService(
            LogEventFactory eventFactory,
            LogEventRepository eventRepository,
            MeterRegistry meterRegistry) {
        this.eventFactory = eventFactory;
        this.eventRepository = eventRepository;
        acceptedCounter =
                Counter.builder("log_analysis.events.ingested")
                        .description("Accepted normalized log events")
                        .register(meterRegistry);
        rejectedCounter =
                Counter.builder("log_analysis.events.rejected")
                        .description("Rejected raw log lines")
                        .register(meterRegistry);
    }

    @Transactional
    public BatchIngestionResult ingest(
            List<NumberedLogLine> lines,
            LogFormat format,
            ParseHints hints,
            LogImportEntity logImport) {
        List<LogEvent> accepted = new ArrayList<>();
        List<LineRejection> rejected = new ArrayList<>();
        UUID importId = logImport == null ? null : logImport.getId();

        for (NumberedLogLine line : lines) {
            try {
                LogEvent event =
                        eventFactory.create(line.value(), format, hints, importId, line.number());
                if (event.externalId() != null
                        && eventRepository.existsBySourceAndExternalId(
                                event.source(), event.externalId())) {
                    rejected.add(
                            new LineRejection(
                                    line.number(),
                                    "duplicate_external_id",
                                    "An event with this source and external identifier already exists.",
                                    format));
                    continue;
                }
                accepted.add(event);
            } catch (LogParsingException error) {
                rejected.add(
                        new LineRejection(
                                line.number(), error.code(), error.getMessage(), error.format()));
            } catch (IllegalArgumentException error) {
                rejected.add(
                        new LineRejection(
                                line.number(),
                                "invalid_event",
                                "Normalized event validation failed.",
                                format));
            }
        }

        List<LogEventEntity> entities =
                accepted.stream()
                        .map(event -> new LogEventEntity(event, logImport, event.lineNumber()))
                        .toList();
        eventRepository.saveAll(entities);
        acceptedCounter.increment(accepted.size());
        rejectedCounter.increment(rejected.size());
        return new BatchIngestionResult(accepted, rejected);
    }
}
