package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.LogFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class ApplicationLogParser implements LogLineParser {

    private static final Pattern APPLICATION_PATTERN =
            Pattern.compile(
                    "^(?<timestamp>\\S+)\\s+"
                            + "(?<level>TRACE|DEBUG|INFO|WARN|WARNING|ERROR|FATAL|CRITICAL)\\s+"
                            + "(?:\\[(?<service>[^]]+)])?\\s*"
                            + "(?:\\[(?<source>[^]]+)])?\\s*"
                            + "(?<message>.+)$",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_VALUE_PATTERN =
            Pattern.compile(
                    "(?<key>[A-Za-z][A-Za-z0-9_.-]*)=(?:\"(?<quoted>[^\"]*)\"|(?<plain>\\S+))");

    @Override
    public LogFormat format() {
        return LogFormat.APPLICATION;
    }

    @Override
    public boolean supports(String line) {
        return APPLICATION_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedLogLine parse(String line, ParseHints hints) {
        Matcher matcher = APPLICATION_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new LogParsingException(
                    "invalid_application_log",
                    format(),
                    "Application log must contain timestamp, level, and message.");
        }
        Map<String, String> attributes = keyValues(matcher.group("message"));
        return new ParsedLogLine(
                format(),
                ParserSupport.parseInstant(matcher.group("timestamp"), format()),
                ParserSupport.firstPresent(
                        matcher.group("source"), hints.source(), "source", format()),
                ParserSupport.firstPresent(
                        matcher.group("service"), hints.service(), "service", format()),
                ParserSupport.parseSeverity(matcher.group("level"), format()),
                attributes.get("event_type"),
                matcher.group("message"),
                firstNonBlank(attributes.get("trace_id"), attributes.get("correlation_id")),
                firstNonBlank(attributes.get("subject_id"), attributes.get("user_id")),
                attributes.get("event_id"),
                attributes);
    }

    private static Map<String, String> keyValues(String message) {
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher matcher = KEY_VALUE_PATTERN.matcher(message);
        while (matcher.find()) {
            String value = matcher.group("quoted");
            attributes.put(matcher.group("key"), value == null ? matcher.group("plain") : value);
        }
        return attributes;
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
