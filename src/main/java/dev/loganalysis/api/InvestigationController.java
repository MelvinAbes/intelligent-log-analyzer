package dev.loganalysis.api;

import dev.loganalysis.api.model.IncidentPageResponse;
import dev.loganalysis.api.model.IncidentResponse;
import dev.loganalysis.api.model.IncidentSummaryResponse;
import dev.loganalysis.api.model.LogEventPageResponse;
import dev.loganalysis.api.model.LogEventResponse;
import dev.loganalysis.api.model.UpdateIncidentStatusRequest;
import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.incident.domain.IncidentStatus;
import dev.loganalysis.incident.summary.IncidentSummaryService;
import dev.loganalysis.query.IncidentQueryService;
import dev.loganalysis.query.LogSearchCriteria;
import dev.loganalysis.query.LogSearchService;
import dev.loganalysis.statistics.LogStatistics;
import dev.loganalysis.statistics.LogStatisticsService;
import dev.loganalysis.statistics.StatisticsBucket;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class InvestigationController {

    private final LogSearchService logSearch;
    private final IncidentQueryService incidents;
    private final LogStatisticsService statistics;
    private final IncidentSummaryService summaries;

    public InvestigationController(
            LogSearchService logSearch,
            IncidentQueryService incidents,
            LogStatisticsService statistics,
            IncidentSummaryService summaries) {
        this.logSearch = logSearch;
        this.incidents = incidents;
        this.statistics = statistics;
        this.summaries = summaries;
    }

    @GetMapping("/log-events")
    public LogEventPageResponse searchEvents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) EventSeverity severity,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return LogEventPageResponse.from(
                logSearch.search(
                        new LogSearchCriteria(
                                from, to, source, service, severity, query, page, size)));
    }

    @GetMapping("/incidents")
    public IncidentPageResponse searchIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentSeverity severity,
            @RequestParam(required = false) String ruleCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return IncidentPageResponse.from(
                incidents.search(from, to, status, severity, ruleCode, page, size));
    }

    @GetMapping("/incidents/{id}")
    public IncidentResponse getIncident(@PathVariable UUID id) {
        return IncidentResponse.from(incidents.get(id));
    }

    @GetMapping("/incidents/{id}/timeline")
    public List<LogEventResponse> timeline(@PathVariable UUID id) {
        return incidents.timeline(id).stream().map(LogEventResponse::from).toList();
    }

    @PostMapping("/incidents/{id}/summary")
    public IncidentSummaryResponse summarize(@PathVariable UUID id) {
        return IncidentSummaryResponse.from(summaries.summarize(id));
    }

    @PatchMapping("/incidents/{id}")
    public IncidentResponse updateStatus(
            @PathVariable UUID id, @Valid @RequestBody UpdateIncidentStatusRequest request) {
        return IncidentResponse.from(incidents.setStatus(id, request.status()));
    }

    @GetMapping("/statistics")
    public LogStatistics statistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant to,
            @RequestParam(defaultValue = "HOUR") StatisticsBucket bucket) {
        return statistics.calculate(from, to, bucket);
    }
}
