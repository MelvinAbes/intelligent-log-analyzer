package dev.loganalysis.incident.summary;

public final class DisabledIncidentSummaryProvider implements IncidentSummaryProvider {

    @Override
    public String name() {
        return "disabled";
    }

    @Override
    public boolean enabled() {
        return false;
    }

    @Override
    public String summarize(IncidentSummaryRequest request) {
        throw new IllegalStateException("Assisted summaries are disabled.");
    }
}
