package dev.loganalysis.importing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.persistence.entity.LogImportEntity;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class LogImportEntityTest {

    @Test
    void recordsProgressOnlyWhileProcessing() {
        Instant createdAt = Instant.parse("2026-07-30T10:00:00Z");
        LogImportEntity logImport =
                LogImportEntity.queued(
                        "device.log",
                        LogFormat.SYSLOG,
                        "imports/source.log",
                        "edge-1",
                        "device-agent",
                        createdAt);

        logImport.start(createdAt.plusSeconds(1), createdAt.plus(Duration.ofMinutes(2)));
        logImport.recordBatch(10, 8, 2, createdAt.plus(Duration.ofMinutes(3)));
        logImport.complete(createdAt.plus(Duration.ofMinutes(1)));

        assertThat(logImport.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(logImport.getTotalLines()).isEqualTo(10);
        assertThat(logImport.getAcceptedLines()).isEqualTo(8);
        assertThat(logImport.getRejectedLines()).isEqualTo(2);
    }

    @Test
    void completedImportCannotFail() {
        Instant now = Instant.parse("2026-07-30T10:00:00Z");
        LogImportEntity logImport =
                LogImportEntity.queued(
                        "events.jsonl", LogFormat.JSON_LINES, "imports/events", null, null, now);
        logImport.start(now, now.plusSeconds(30));
        logImport.complete(now.plusSeconds(1));

        assertThatThrownBy(() -> logImport.fail("storage_error", now.plusSeconds(2)))
                .isInstanceOf(IllegalStateException.class);
    }
}
