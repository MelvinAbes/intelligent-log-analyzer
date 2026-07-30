package dev.loganalysis.importing;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.ingestion.NumberedLogLine;
import dev.loganalysis.persistence.entity.LogImportEntity;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "analysis.imports",
        name = "worker-enabled",
        havingValue = "true",
        matchIfMissing = true)
public class LogImportWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogImportWorker.class);

    private final LogImportCoordinator coordinator;
    private final ImportBatchProcessor batchProcessor;
    private final LogImportStorage storage;
    private final AnalysisProperties properties;

    public LogImportWorker(
            LogImportCoordinator coordinator,
            ImportBatchProcessor batchProcessor,
            LogImportStorage storage,
            AnalysisProperties properties) {
        this.coordinator = coordinator;
        this.batchProcessor = batchProcessor;
        this.storage = storage;
        this.properties = properties;
    }

    @Scheduled(
            fixedDelayString = "${analysis.imports.poll-interval-ms:1000}",
            initialDelayString = "${analysis.imports.initial-delay-ms:1000}")
    public void scheduledPoll() {
        processNext();
    }

    public boolean processNext() {
        Optional<UUID> claimed = coordinator.claimNext();
        if (claimed.isEmpty()) {
            return false;
        }
        process(claimed.orElseThrow());
        return true;
    }

    private void process(UUID importId) {
        LogImportEntity logImport = coordinator.get(importId);
        try (BufferedReader reader = storage.open(logImport.getStorageKey())) {
            skipProcessedLines(reader, logImport.getTotalLines());
            if (!readBatches(reader, logImport)) {
                return;
            }
            coordinator.complete(importId);
            try {
                storage.delete(logImport.getStorageKey());
            } catch (IOException cleanupError) {
                LOGGER.warn("completed import source cleanup failed importId={}", importId);
            }
        } catch (IOException error) {
            coordinator.fail(importId, "import_read_failed");
            LOGGER.warn("log import failed importId={} code=import_read_failed", importId);
        }
    }

    private boolean readBatches(BufferedReader reader, LogImportEntity logImport)
            throws IOException {
        List<NumberedLogLine> batch = new ArrayList<>(properties.imports().batchSize());
        int lineNumber = logImport.getTotalLines();
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            if (lineNumber > properties.imports().maxLines()) {
                coordinator.fail(logImport.getId(), "import_line_limit_exceeded");
                return false;
            }
            String boundedLine = line.length() <= properties.imports().maxLineLength() ? line : "";
            batch.add(new NumberedLogLine(lineNumber, boundedLine));
            if (batch.size() == properties.imports().batchSize()) {
                batchProcessor.process(logImport.getId(), List.copyOf(batch));
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            batchProcessor.process(logImport.getId(), List.copyOf(batch));
        }
        return true;
    }

    private static void skipProcessedLines(BufferedReader reader, int processed)
            throws IOException {
        for (int index = 0; index < processed; index++) {
            if (reader.readLine() == null) {
                throw new IOException("stored import ended before recorded progress");
            }
        }
    }
}
