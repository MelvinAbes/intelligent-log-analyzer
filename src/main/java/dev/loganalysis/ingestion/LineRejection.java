package dev.loganalysis.ingestion;

import dev.loganalysis.event.domain.LogFormat;

public record LineRejection(int position, String code, String message, LogFormat format) {}
