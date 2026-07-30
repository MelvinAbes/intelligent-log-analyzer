package dev.loganalysis.api.model;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.importing.domain.ImportStatus;
import dev.loganalysis.persistence.entity.LogImportEntity;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LogImportResponse(
        UUID id,
        String originalFilename,
        LogFormat format,
        ImportStatus status,
        int totalLines,
        int acceptedLines,
        int rejectedLines,
        String failureCode,
        List<Map<String, String>> rejectionSamples,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt) {

    public static LogImportResponse from(LogImportEntity logImport) {
        return new LogImportResponse(
                logImport.getId(),
                logImport.getOriginalFilename(),
                logImport.getFormat(),
                logImport.getStatus(),
                logImport.getTotalLines(),
                logImport.getAcceptedLines(),
                logImport.getRejectedLines(),
                logImport.getFailureCode(),
                logImport.getRejectionSamples(),
                logImport.getCreatedAt(),
                logImport.getStartedAt(),
                logImport.getCompletedAt());
    }
}
