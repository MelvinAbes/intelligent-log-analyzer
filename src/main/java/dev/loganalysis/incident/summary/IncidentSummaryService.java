package dev.loganalysis.incident.summary;

import dev.loganalysis.query.IncidentQueryService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IncidentSummaryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(IncidentSummaryService.class);

    private final IncidentQueryService incidents;
    private final IncidentSummaryProvider provider;

    public IncidentSummaryService(
            IncidentQueryService incidents, IncidentSummaryProvider provider) {
        this.incidents = incidents;
        this.provider = provider;
    }

    public IncidentSummaryResult summarize(UUID incidentId) {
        var incident = incidents.get(incidentId);
        if (!provider.enabled()) {
            return new IncidentSummaryResult(
                    "deterministic", provider.name(), incident.getSummary(), "provider_disabled");
        }

        try {
            String summary =
                    provider.summarize(
                            new IncidentSummaryRequest(incident, incidents.timeline(incidentId)));
            return new IncidentSummaryResult("assisted", provider.name(), summary, null);
        } catch (RuntimeException error) {
            LOGGER.atWarn()
                    .setCause(error)
                    .addKeyValue("incident_id", incidentId)
                    .log("Assisted incident summary failed; returning deterministic summary");
            return new IncidentSummaryResult(
                    "deterministic",
                    provider.name(),
                    incident.getSummary(),
                    "provider_unavailable");
        }
    }
}
