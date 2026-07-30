package dev.loganalysis.importing;

public class InvalidImportException extends RuntimeException {

    private final String code;

    public InvalidImportException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() {
        return code;
    }
}
