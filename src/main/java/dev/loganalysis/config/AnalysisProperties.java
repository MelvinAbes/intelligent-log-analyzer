package dev.loganalysis.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.file.Path;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "analysis")
public record AnalysisProperties(
        @Valid @NotNull ImportSettings imports,
        @Valid @NotNull DetectionSettings detection,
        @Valid @NotNull SummarySettings summary) {

    public record ImportSettings(
            @NotNull Path storagePath,
            @Min(1) @Max(1_000_000) int maxLines,
            @Min(256) @Max(65_536) int maxLineLength,
            @Min(1) @Max(5_000) int batchSize,
            @NotNull Duration leaseDuration) {}

    public record DetectionSettings(
            @Min(2) int repeatedErrorThreshold,
            @NotNull Duration repeatedErrorWindow,
            @Min(2) int spikeMinimumCount,
            @DecimalMin("1.1") double spikeMultiplier,
            @NotNull Duration sequenceWindow) {}

    public record SummarySettings(
            @NotBlank String provider,
            @NotBlank String baseUrl,
            @NotBlank String model,
            String apiToken) {}
}
