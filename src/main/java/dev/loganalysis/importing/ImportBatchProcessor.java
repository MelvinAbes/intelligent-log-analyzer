package dev.loganalysis.importing;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.ingestion.BatchIngestionResult;
import dev.loganalysis.ingestion.LogEventIngestionService;
import dev.loganalysis.ingestion.NumberedLogLine;
import dev.loganalysis.parsing.ParseHints;
import dev.loganalysis.persistence.entity.LogImportEntity;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImportBatchProcessor {

    private final LogImportRepository importRepository;
    private final LogEventIngestionService ingestionService;
    private final AnalysisProperties properties;
    private final Clock clock;

    public ImportBatchProcessor(
            LogImportRepository importRepository,
            LogEventIngestionService ingestionService,
            AnalysisProperties properties,
            Clock clock) {
        this.importRepository = importRepository;
        this.ingestionService = ingestionService;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public BatchIngestionResult process(UUID importId, List<NumberedLogLine> lines) {
        LogImportEntity logImport =
                importRepository
                        .findById(importId)
                        .orElseThrow(() -> new LogImportNotFoundException(importId));
        BatchIngestionResult result =
                ingestionService.ingest(
                        lines,
                        logImport.getFormat(),
                        new ParseHints(logImport.getSourceHint(), logImport.getServiceHint()),
                        logImport);
        List<Map<String, String>> samples =
                result.rejections().stream()
                        .map(
                                rejection ->
                                        Map.of(
                                                "line",
                                                Integer.toString(rejection.position()),
                                                "code",
                                                rejection.code(),
                                                "message",
                                                rejection.message()))
                        .toList();
        logImport.recordBatch(
                lines.size(),
                result.acceptedCount(),
                result.rejectedCount(),
                samples,
                clock.instant().plus(properties.imports().leaseDuration()));
        return result;
    }
}
