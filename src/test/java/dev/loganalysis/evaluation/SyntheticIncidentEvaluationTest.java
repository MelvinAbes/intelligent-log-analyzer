package dev.loganalysis.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.loganalysis.TestcontainersConfiguration;
import dev.loganalysis.event.domain.LogFormat;
import dev.loganalysis.ingestion.LogEventIngestionService;
import dev.loganalysis.ingestion.NumberedLogLine;
import dev.loganalysis.parsing.ParseHints;
import dev.loganalysis.persistence.entity.IncidentEntity;
import dev.loganalysis.persistence.repository.IncidentEventRepository;
import dev.loganalysis.persistence.repository.IncidentRepository;
import dev.loganalysis.persistence.repository.LogEventRepository;
import dev.loganalysis.persistence.repository.LogImportRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Tag("evaluation")
@SpringBootTest(properties = "analysis.imports.worker-enabled=false")
@Import(TestcontainersConfiguration.class)
class SyntheticIncidentEvaluationTest {

    private static final Path SAMPLE_ROOT = Path.of("samples");

    @Autowired private LogEventIngestionService ingestion;
    @Autowired private IncidentRepository incidentRepository;
    @Autowired private IncidentEventRepository incidentEventRepository;
    @Autowired private LogEventRepository eventRepository;
    @Autowired private LogImportRepository importRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        incidentEventRepository.deleteAll();
        incidentRepository.deleteAll();
        eventRepository.deleteAll();
        importRepository.deleteAll();
    }

    @Test
    void evaluatesLabelledIncidentSignals() throws IOException {
        int accepted = 0;
        accepted += ingest("logs/application-errors.log", LogFormat.APPLICATION, null, null);
        accepted += ingest("logs/gateway-access.log", LogFormat.ACCESS, null, "edge-gateway");
        accepted += ingest("logs/security-device.log", LogFormat.SYSLOG, null, null);
        accepted += ingest("logs/normal-events.jsonl", LogFormat.JSON_LINES, null, null);

        List<ExpectedIncident> expected =
                objectMapper.readValue(
                        SAMPLE_ROOT.resolve("evaluation/expected-incidents.json").toFile(),
                        new TypeReference<>() {});
        List<IncidentEntity> actual = incidentRepository.findAll();
        List<String> matchedLabels = new ArrayList<>();
        List<String> missedLabels = new ArrayList<>();
        List<String> unexpectedDetections = new ArrayList<>();

        for (ExpectedIncident label : expected) {
            boolean matched = actual.stream().anyMatch(incident -> label.matches(incident));
            (matched ? matchedLabels : missedLabels).add(label.label());
        }
        for (IncidentEntity incident : actual) {
            boolean expectedDetection =
                    expected.stream().anyMatch(label -> label.matches(incident));
            if (!expectedDetection) {
                unexpectedDetections.add(incident.getRuleCode() + ":" + incident.getGroupingKey());
            }
        }

        int truePositives = matchedLabels.size();
        int falsePositives = unexpectedDetections.size();
        int falseNegatives = missedLabels.size();
        double precision = ratio(truePositives, truePositives + falsePositives);
        double recall = ratio(truePositives, truePositives + falseNegatives);
        double f1 = harmonicMean(precision, recall);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("evaluatedAt", Instant.now().toString());
        report.put("acceptedEvents", accepted);
        report.put("expectedIncidents", expected.size());
        report.put("detectedIncidents", actual.size());
        report.put("truePositives", truePositives);
        report.put("falsePositives", falsePositives);
        report.put("falseNegatives", falseNegatives);
        report.put("precision", precision);
        report.put("recall", recall);
        report.put("f1", f1);
        report.put("matchedLabels", matchedLabels);
        report.put("missedLabels", missedLabels);
        report.put("unexpectedDetections", unexpectedDetections);
        writeReport(report);

        assertThat(accepted).isEqualTo(30);
        assertThat(missedLabels).isEmpty();
        assertThat(unexpectedDetections).isEmpty();
        assertThat(precision).isEqualTo(1.0);
        assertThat(recall).isEqualTo(1.0);
        assertThat(f1).isEqualTo(1.0);
    }

    private int ingest(String relativePath, LogFormat format, String source, String service)
            throws IOException {
        List<String> lines = Files.readAllLines(SAMPLE_ROOT.resolve(relativePath));
        List<NumberedLogLine> numbered = new ArrayList<>(lines.size());
        for (int index = 0; index < lines.size(); index++) {
            numbered.add(new NumberedLogLine(index + 1, lines.get(index)));
        }
        var result = ingestion.ingest(numbered, format, new ParseHints(source, service), null);
        assertThat(result.rejections()).as(relativePath).isEmpty();
        return result.acceptedCount();
    }

    private void writeReport(Map<String, Object> report) throws IOException {
        Path output =
                Path.of(
                        System.getProperty(
                                "evaluation.output",
                                "build/reports/evaluation/incident-detection.json"));
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
    }

    private static double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private static double harmonicMean(double precision, double recall) {
        return precision + recall == 0 ? 0.0 : 2 * precision * recall / (precision + recall);
    }

    record ExpectedIncident(
            String label, String ruleCode, String evidenceKey, String evidenceValue) {

        boolean matches(IncidentEntity incident) {
            Object evidence = incident.getEvidence().get(evidenceKey);
            return incident.getRuleCode().equals(ruleCode)
                    && evidence != null
                    && evidence.toString().equals(evidenceValue);
        }
    }
}
