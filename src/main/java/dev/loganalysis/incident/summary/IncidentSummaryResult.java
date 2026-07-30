package dev.loganalysis.incident.summary;

public record IncidentSummaryResult(
        String mode, String provider, String summary, String fallbackReason) {}
