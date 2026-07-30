package dev.loganalysis.statistics;

import java.time.Instant;

public record TimeBucketCount(Instant bucketStart, long count) {}
