package dev.loganalysis.api.model;

import dev.loganalysis.event.domain.LogFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record IngestLogBatchRequest(
        @NotNull LogFormat format,
        @Size(max = 128) String source,
        @Size(max = 128) String service,
        @NotEmpty @Size(max = 500) List<@Valid RawLogEntryRequest> entries) {}
