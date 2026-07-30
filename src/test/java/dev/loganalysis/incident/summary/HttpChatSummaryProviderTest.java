package dev.loganalysis.incident.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.incident.domain.IncidentSeverity;
import dev.loganalysis.persistence.entity.IncidentEntity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class HttpChatSummaryProviderTest {

    @Test
    void sendsBoundedEvidenceAndReadsSummary() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        var settings =
                new AnalysisProperties.SummarySettings(
                        "chat-http",
                        "http://summary.test/v1",
                        "local-model",
                        "test-token",
                        Duration.ofSeconds(2));
        var provider = new HttpChatSummaryProvider(builder, settings);
        var incident =
                new IncidentEntity(
                        "repeated_error",
                        "payments:fingerprint",
                        IncidentSeverity.HIGH,
                        "Repeated errors in payments",
                        "Payments logged five matching errors.",
                        Instant.parse("2026-07-30T10:00:00Z"),
                        Instant.parse("2026-07-30T10:00:00Z"),
                        Instant.parse("2026-07-30T10:04:00Z"),
                        Instant.parse("2026-07-30T10:05:00Z"),
                        5,
                        Map.of("observed_count", 5));

        server.expect(requestTo("http://summary.test/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(
                        withSuccess(
                                """
                                {"choices":[{"message":{"role":"assistant","content":"\
                                Five payment errors occurred. Verify the downstream gateway."}}]}
                                """,
                                MediaType.APPLICATION_JSON));

        String summary = provider.summarize(new IncidentSummaryRequest(incident, List.of()));

        assertThat(summary)
                .isEqualTo("Five payment errors occurred. Verify the downstream gateway.");
        server.verify();
    }
}
