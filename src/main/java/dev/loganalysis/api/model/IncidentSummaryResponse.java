package dev.loganalysis.api.model;

import dev.loganalysis.incident.summary.IncidentSummaryResult;

public record IncidentSummaryResponse(
        String mode, String provider, String summary, String fallbackReason) {

    public static IncidentSummaryResponse from(IncidentSummaryResult result) {
        return new IncidentSummaryResponse(
                result.mode(), result.provider(), result.summary(), result.fallbackReason());
    }
}
