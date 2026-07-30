package dev.loganalysis.incident.analysis;

import dev.loganalysis.incident.summary.DeterministicIncidentSummarizer;
import dev.loganalysis.persistence.entity.IncidentEntity;
import dev.loganalysis.persistence.entity.IncidentEventEntity;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.IncidentEventRepository;
import dev.loganalysis.persistence.repository.IncidentRepository;
import dev.loganalysis.persistence.repository.LogEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IncidentReconciler {

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final LogEventRepository eventRepository;
    private final DeterministicIncidentSummarizer summarizer;
    private final Clock clock;

    public IncidentReconciler(
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            LogEventRepository eventRepository,
            DeterministicIncidentSummarizer summarizer,
            Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.eventRepository = eventRepository;
        this.summarizer = summarizer;
        this.clock = clock;
    }

    public UUID reconcile(IncidentCandidate candidate) {
        var narrative = summarizer.summarize(candidate);
        Instant now = clock.instant();
        IncidentEntity incident =
                incidentRepository
                        .findByRuleCodeAndGroupingKeyAndWindowStart(
                                candidate.ruleCode(),
                                candidate.groupingKey(),
                                candidate.windowStart())
                        .orElseGet(
                                () ->
                                        new IncidentEntity(
                                                candidate.ruleCode(),
                                                candidate.groupingKey(),
                                                candidate.severity(),
                                                narrative.title(),
                                                narrative.summary(),
                                                candidate.windowStart(),
                                                candidate.startedAt(),
                                                candidate.endedAt(),
                                                now,
                                                candidate.eventIds().size(),
                                                candidate.evidence()));
        if (incident.getId() != null) {
            incident.update(
                    candidate.severity(),
                    narrative.title(),
                    narrative.summary(),
                    candidate.startedAt(),
                    candidate.endedAt(),
                    now,
                    candidate.eventIds().size(),
                    candidate.evidence());
        }
        incident = incidentRepository.save(incident);

        List<LogEventEntity> events = eventRepository.findAllById(candidate.eventIds());
        for (LogEventEntity event : events) {
            if (!incidentEventRepository.existsByIncidentIdAndEventId(
                    incident.getId(), event.getId())) {
                incidentEventRepository.save(new IncidentEventEntity(incident, event, "evidence"));
            }
        }
        return incident.getId();
    }
}
