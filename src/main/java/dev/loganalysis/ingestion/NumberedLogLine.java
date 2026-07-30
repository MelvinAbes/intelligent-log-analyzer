package dev.loganalysis.ingestion;

public record NumberedLogLine(int number, String value) {

    public NumberedLogLine {
        if (number < 1) {
            throw new IllegalArgumentException("line number must be positive");
        }
    }
}
