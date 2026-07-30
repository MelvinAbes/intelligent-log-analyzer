package dev.loganalysis.query;

import java.util.UUID;

public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(UUID id) {
        super("Incident " + id + " was not found.");
    }
}
