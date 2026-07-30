package dev.loganalysis.incident.domain;

public enum IncidentSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public boolean atLeast(IncidentSeverity other) {
        return ordinal() >= other.ordinal();
    }
}
