package dev.loganalysis.incident.summary;

import dev.loganalysis.config.AnalysisProperties;
import dev.loganalysis.event.domain.LogEvent;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

public final class HttpChatSummaryProvider implements IncidentSummaryProvider {

    private static final int MAX_EVENTS = 30;
    private static final int MAX_RESPONSE_LENGTH = 4096;

    private final RestClient client;
    private final AnalysisProperties.SummarySettings settings;

    public HttpChatSummaryProvider(
            RestClient.Builder clientBuilder, AnalysisProperties.SummarySettings settings) {
        RestClient.Builder configured = clientBuilder.baseUrl(settings.baseUrl());
        if (StringUtils.hasText(settings.apiToken())) {
            configured.defaultHeader("Authorization", "Bearer " + settings.apiToken().strip());
        }
        this.client = configured.build();
        this.settings = settings;
    }

    @Override
    public String name() {
        return "chat-http";
    }

    @Override
    public boolean enabled() {
        return true;
    }

    @Override
    public String summarize(IncidentSummaryRequest request) {
        CompletionResponse response =
                client.post()
                        .uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(toCompletionRequest(request))
                        .retrieve()
                        .body(CompletionResponse.class);
        String content = extractContent(response);
        return content.length() <= MAX_RESPONSE_LENGTH
                ? content
                : content.substring(0, MAX_RESPONSE_LENGTH);
    }

    private CompletionRequest toCompletionRequest(IncidentSummaryRequest request) {
        var incident = request.incident();
        String evidence =
                """
                Rule: %s
                Severity: %s
                Title: %s
                Deterministic summary: %s
                Window: %s to %s
                Event count: %d
                Evidence: %s
                Timeline:
                %s
                """
                        .formatted(
                                incident.getRuleCode(),
                                incident.getSeverity(),
                                incident.getTitle(),
                                incident.getSummary(),
                                incident.getStartedAt(),
                                incident.getEndedAt(),
                                incident.getEventCount(),
                                incident.getEvidence(),
                                timelineText(request.timeline()));
        return new CompletionRequest(
                settings.model(),
                List.of(
                        new Message(
                                "system",
                                "Write a concise operational incident summary from the supplied "
                                        + "evidence. State only supported facts and suggest one "
                                        + "verification step."),
                        new Message("user", evidence)),
                0.1,
                false);
    }

    private static String timelineText(List<LogEvent> timeline) {
        return timeline.stream()
                .limit(MAX_EVENTS)
                .map(
                        event ->
                                "%s | %s | %s | %s | %s"
                                        .formatted(
                                                event.occurredAt(),
                                                event.severity(),
                                                event.service(),
                                                event.source(),
                                                event.message()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse("No linked events.");
    }

    private static String extractContent(CompletionResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().getFirst().message() == null
                || !StringUtils.hasText(response.choices().getFirst().message().content())) {
            throw new IllegalStateException("Summary provider returned no content.");
        }
        return response.choices().getFirst().message().content().strip();
    }

    record CompletionRequest(
            String model, List<Message> messages, double temperature, boolean stream) {}

    record CompletionResponse(List<Choice> choices) {}

    record Choice(Message message) {}

    record Message(String role, String content) {}
}
