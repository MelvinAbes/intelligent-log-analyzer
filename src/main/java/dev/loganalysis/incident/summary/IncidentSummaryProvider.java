package dev.loganalysis.incident.summary;

public interface IncidentSummaryProvider {

    String name();

    boolean enabled();

    String summarize(IncidentSummaryRequest request);
}
