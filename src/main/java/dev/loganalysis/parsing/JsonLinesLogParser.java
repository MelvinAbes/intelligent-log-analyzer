package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class JsonLinesLogParser implements LogLineParser {

    private final ObjectMapper objectMapper;

    public JsonLinesLogParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public LogFormat format() {
        return LogFormat.JSON_LINES;
    }

    @Override
    public boolean supports(String line) {
        String stripped = line.strip();
        return stripped.startsWith("{") && stripped.endsWith("}");
    }

    @Override
    public ParsedLogLine parse(String line, ParseHints hints) {
        JsonNode root;
        try {
            root = objectMapper.readTree(line);
        } catch (JacksonException error) {
            throw new LogParsingException(
                    "invalid_json", format(), "Log line is not valid JSON.", error);
        }
        if (!root.isObject()) {
            throw new LogParsingException(
                    "invalid_json_shape", format(), "JSON log line must be an object.");
        }

        String timestamp = text(root, "timestamp", "@timestamp", "time");
        if (timestamp == null) {
            throw new LogParsingException(
                    "missing_timestamp", format(), "JSON log timestamp is required.");
        }
        Instant occurredAt = ParserSupport.parseInstant(timestamp, format());
        String source =
                ParserSupport.firstPresent(
                        text(root, "source", "host", "device"), hints.source(), "source", format());
        String service =
                ParserSupport.firstPresent(
                        text(root, "service", "application", "app"),
                        hints.service(),
                        "service",
                        format());
        String message = text(root, "message", "msg");
        if (message == null || message.isBlank()) {
            throw new LogParsingException(
                    "missing_message", format(), "JSON log message is required.");
        }
        EventSeverity severity =
                ParserSupport.parseSeverity(text(root, "severity", "level"), format());

        return new ParsedLogLine(
                format(),
                occurredAt,
                source,
                service,
                severity,
                text(root, "event_type", "eventType", "type"),
                message,
                text(root, "trace_id", "traceId", "correlation_id"),
                text(root, "subject_id", "subjectId", "user_id"),
                text(root, "external_id", "externalId", "id"),
                attributes(root.get("attributes")));
    }

    private static String text(JsonNode root, String... names) {
        for (String name : names) {
            JsonNode value = root.get(name);
            if (value != null && value.isValueNode() && !value.isNull()) {
                String text = value.asString().strip();
                if (!text.isEmpty()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static Map<String, String> attributes(JsonNode node) {
        if (node == null || !node.isObject()) {
            return Map.of();
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> property : node.properties()) {
            if (property.getValue().isValueNode() && !property.getValue().isNull()) {
                attributes.put(property.getKey(), property.getValue().asString());
            }
        }
        return attributes;
    }
}
