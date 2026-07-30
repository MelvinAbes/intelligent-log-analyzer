package dev.loganalysis.api.model;

import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.ingestion.LineRejection;

public record LineRejectionResponse(int position, String code, String message, LogFormat format) {

    public static LineRejectionResponse from(LineRejection rejection) {
        return new LineRejectionResponse(
                rejection.position(), rejection.code(), rejection.message(), rejection.format());
    }
}
