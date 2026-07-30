package dev.loganalysis.api.model;

import dev.loganalysis.incident.domain.IncidentStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateIncidentStatusRequest(@NotNull IncidentStatus status) {}
