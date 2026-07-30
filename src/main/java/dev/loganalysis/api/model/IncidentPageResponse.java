package dev.loganalysis.api.model;

import dev.loganalysis.query.IncidentPage;
import java.util.List;

public record IncidentPageResponse(
        List<IncidentResponse> items, int page, int size, long totalElements, int totalPages) {

    public static IncidentPageResponse from(IncidentPage result) {
        return new IncidentPageResponse(
                result.items().stream().map(IncidentResponse::from).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages());
    }
}
