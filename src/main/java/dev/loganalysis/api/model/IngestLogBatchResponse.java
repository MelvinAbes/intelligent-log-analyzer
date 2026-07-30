package dev.loganalysis.api.model;

import dev.loganalysis.ingestion.BatchIngestionResult;
import java.util.List;

public record IngestLogBatchResponse(
        int acceptedCount,
        int rejectedCount,
        List<LogEventResponse> events,
        List<LineRejectionResponse> rejections) {

    public static IngestLogBatchResponse from(BatchIngestionResult result) {
        return new IngestLogBatchResponse(
                result.acceptedCount(),
                result.rejectedCount(),
                result.events().stream().map(LogEventResponse::from).toList(),
                result.rejections().stream().map(LineRejectionResponse::from).toList());
    }
}
