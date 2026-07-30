package dev.loganalysis.api.model;

import dev.loganalysis.query.LogSearchPage;
import java.util.List;

public record LogEventPageResponse(
        List<LogEventResponse> items, int page, int size, long totalElements, int totalPages) {

    public static LogEventPageResponse from(LogSearchPage result) {
        return new LogEventPageResponse(
                result.items().stream().map(LogEventResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
