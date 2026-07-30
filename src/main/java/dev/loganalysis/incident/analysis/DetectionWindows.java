package dev.loganalysis.incident.analysis;

import java.time.Duration;
import java.time.Instant;

final class DetectionWindows {

    private DetectionWindows() {}

    static Instant floor(Instant instant, Duration window) {
        long windowSeconds = window.toSeconds();
        if (windowSeconds < 1) {
            throw new IllegalArgumentException("detection window must be at least one second");
        }
        return Instant.ofEpochSecond(
                Math.floorDiv(instant.getEpochSecond(), windowSeconds) * windowSeconds);
    }
}
