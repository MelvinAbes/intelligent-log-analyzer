package dev.loganalysis.api.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RawLogEntryRequest(@NotBlank @Size(max = 16_384) String line) {}
