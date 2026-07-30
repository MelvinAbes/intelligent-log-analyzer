package dev.loganalysis.event.domain;

import java.util.Locale;

public enum EventSeverity {
    TRACE(0),
    DEBUG(1),
    INFO(2),
    WARN(3),
    ERROR(4),
    FATAL(5);

    private final int weight;

    EventSeverity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    public static EventSeverity parse(String value) {
        if (value == null || value.isBlank()) {
            return INFO;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "WARNING" -> WARN;
            case "ERR", "SEVERE" -> ERROR;
            case "CRITICAL", "ALERT", "EMERGENCY" -> FATAL;
            default -> EventSeverity.valueOf(value.trim().toUpperCase(Locale.ROOT));
        };
    }
}
