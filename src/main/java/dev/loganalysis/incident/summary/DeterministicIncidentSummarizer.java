package dev.loganalysis.incident.summary;

import dev.loganalysis.incident.analysis.IncidentCandidate;
import dev.loganalysis.incident.analysis.RepeatedErrorDetector;
import dev.loganalysis.incident.analysis.SpikeDetector;
import dev.loganalysis.incident.analysis.SuspiciousSequenceDetector;
import org.springframework.stereotype.Component;

@Component
public class DeterministicIncidentSummarizer {

    public IncidentNarrative summarize(IncidentCandidate candidate) {
        return switch (candidate.ruleCode()) {
            case RepeatedErrorDetector.RULE_CODE -> repeatedError(candidate);
            case SpikeDetector.RULE_CODE -> spike(candidate);
            case SuspiciousSequenceDetector.RULE_CODE -> suspiciousSequence(candidate);
            default ->
                    throw new IllegalArgumentException(
                            "unsupported incident rule " + candidate.ruleCode());
        };
    }

    private static IncidentNarrative repeatedError(IncidentCandidate candidate) {
        String service = candidate.evidence().get("service").toString();
        String count = candidate.evidence().get("observed_count").toString();
        String threshold = candidate.evidence().get("threshold").toString();
        return new IncidentNarrative(
                "Repeated errors in " + service,
                ("%s logged %s matching errors between %s and %s. "
                                + "Severity is %s because the observed count met or exceeded the threshold of %s.")
                        .formatted(
                                service,
                                count,
                                candidate.startedAt(),
                                candidate.endedAt(),
                                candidate.severity(),
                                threshold));
    }

    private static IncidentNarrative spike(IncidentCandidate candidate) {
        String service = candidate.evidence().get("service").toString();
        String count = candidate.evidence().get("observed_count").toString();
        String baseline = candidate.evidence().get("baseline_median").toString();
        return new IncidentNarrative(
                "Unusual event spike in " + service,
                ("%s produced %s events in one minute; the preceding bucket median was %s. "
                                + "The transparent spike rule assigned severity %s.")
                        .formatted(service, count, baseline, candidate.severity()));
    }

    private static IncidentNarrative suspiciousSequence(IncidentCandidate candidate) {
        String subject = candidate.evidence().get("subject_id").toString();
        String failures = candidate.evidence().get("failed_authentications").toString();
        return new IncidentNarrative(
                "Suspicious privilege sequence for " + subject,
                ("Subject %s had %s failed authentications, then a successful authentication, "
                                + "followed by a privilege change within the configured window. "
                                + "This sequence is classified as CRITICAL.")
                        .formatted(subject, failures));
    }
}
