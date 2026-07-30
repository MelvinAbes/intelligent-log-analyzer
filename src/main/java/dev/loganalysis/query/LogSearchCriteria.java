package dev.loganalysis.query;

import dev.loganalysis.event.domain.EventSeverity;
import java.time.Instant;

public record LogSearchCriteria(
        Instant from,
        Instant to,
        String source,
        String service,
        EventSeverity severity,
        String query,
        int page,
        int size) {}
