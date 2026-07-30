package dev.loganalysis.statistics;

import java.time.Instant;
import java.util.List;

public record LogStatistics(
        Instant from,
        Instant to,
        long totalEvents,
        long openIncidents,
        List<NamedCount> bySeverity,
        List<NamedCount> busiestServices,
        List<TimeBucketCount> timeline) {

    public LogStatistics {
        bySeverity = List.copyOf(bySeverity);
        busiestServices = List.copyOf(busiestServices);
        timeline = List.copyOf(timeline);
    }
}
