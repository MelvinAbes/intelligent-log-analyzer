package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.LogFormat;

public class LogParsingException extends RuntimeException {

    private final String code;
    private final LogFormat format;

    public LogParsingException(String code, LogFormat format, String message) {
        super(message);
        this.code = code;
        this.format = format;
    }

    public LogParsingException(String code, LogFormat format, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.format = format;
    }

    public String code() {
        return code;
    }

    public LogFormat format() {
        return format;
    }
}
