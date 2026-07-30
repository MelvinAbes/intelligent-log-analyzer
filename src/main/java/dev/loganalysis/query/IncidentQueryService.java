package dev.loganalysis.query;

import dev.loganalysis.event.domain.LogEvent;
import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.incident.domain.IncidentStatus;
import dev.loganalysis.persistence.entity.IncidentEntity;
import dev.loganalysis.persistence.entity.LogEventEntity;
import dev.loganalysis.persistence.repository.IncidentEventRepository;
import dev.loganalysis.persistence.repository.IncidentRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentQueryService {

    private static final Instant MINIMUM_TIME = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant MAXIMUM_TIME = Instant.parse("9999-12-31T23:59:59Z");

    private final IncidentRepository incidentRepository;
    private final IncidentEventRepository incidentEventRepository;
    private final Clock clock;

    public IncidentQueryService(
            IncidentRepository incidentRepository,
            IncidentEventRepository incidentEventRepository,
            Clock clock) {
        this.incidentRepository = incidentRepository;
        this.incidentEventRepository = incidentEventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public IncidentPage search(
            Instant from,
            Instant to,
            IncidentStatus status,
            IncidentSeverity severity,
            String ruleCode,
            int pageNumber,
            int pageSize) {
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw new InvalidQueryException("Incident page and size are invalid.");
        }
        if (from != null && to != null && !from.isBefore(to)) {
            throw new InvalidQueryException("Incident search start must be before its end.");
        }
        var page =
                incidentRepository.search(
                        from == null ? MINIMUM_TIME : from,
                        to == null ? MAXIMUM_TIME : to,
                        status,
                        severity,
                        ruleCode == null ? "" : ruleCode.strip(),
                        PageRequest.of(pageNumber, pageSize));
        return new IncidentPage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Transactional(readOnly = true)
    public IncidentEntity get(UUID id) {
        return incidentRepository.findById(id).orElseThrow(() -> new IncidentNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LogEvent> timeline(UUID id) {
        get(id);
        return incidentEventRepository.findTimeline(id).stream()
                .map(LogEventEntity::toDomain)
                .toList();
    }

    @Transactional
    public IncidentEntity setStatus(UUID id, IncidentStatus status) {
        IncidentEntity incident = get(id);
        if (status == IncidentStatus.RESOLVED) {
            incident.resolve(clock.instant());
        } else {
            incident.reopen(clock.instant());
        }
        return incident;
    }
}
