package dev.loganalysis.parsing;

public record ParseHints(String source, String service) {

    public ParseHints {
        source = normalize(source);
        service = normalize(service);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > 128) {
            throw new IllegalArgumentException("parse hint exceeds 128 characters");
        }
        return normalized;
    }
}
