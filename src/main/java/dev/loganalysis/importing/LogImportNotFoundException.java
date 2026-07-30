package dev.loganalysis.importing;

import java.util.UUID;

public class LogImportNotFoundException extends RuntimeException {

    public LogImportNotFoundException(UUID id) {
        super("Log import " + id + " was not found.");
    }
}
