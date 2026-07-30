package dev.loganalysis.incident.analysis;

import java.util.List;

public interface IncidentDetector {

    String ruleCode();

    List<IncidentCandidate> detect(DetectionContext context);
}
