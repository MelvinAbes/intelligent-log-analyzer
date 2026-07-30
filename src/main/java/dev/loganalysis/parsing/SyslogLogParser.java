package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SyslogLogParser implements LogLineParser {

    private static final Pattern SYSLOG_PATTERN =
            Pattern.compile(
                    "^<(?<priority>\\d{1,3})>1\\s+"
                            + "(?<timestamp>\\S+)\\s+(?<host>\\S+)\\s+(?<app>\\S+)\\s+"
                            + "(?<process>\\S+)\\s+(?<messageId>\\S+)\\s+"
                            + "(?<structured>-|(?:\\[[^]]+])+)(?:\\s+(?<message>.*))?$");
    private static final Pattern STRUCTURED_VALUE =
            Pattern.compile("(?<key>[A-Za-z][A-Za-z0-9_.-]*)=\"(?<value>[^\"]*)\"");

    @Override
    public LogFormat format() {
        return LogFormat.SYSLOG;
    }

    @Override
    public boolean supports(String line) {
        return line.startsWith("<") && SYSLOG_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedLogLine parse(String line, ParseHints hints) {
        Matcher matcher = SYSLOG_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new LogParsingException(
                    "invalid_syslog", format(), "Log line is not valid RFC 5424 syslog.");
        }
        int priority = Integer.parseInt(matcher.group("priority"));
        if (priority > 191) {
            throw new LogParsingException(
                    "invalid_syslog_priority",
                    format(),
                    "Syslog priority must be between 0 and 191.");
        }
        Map<String, String> attributes = structuredAttributes(matcher.group("structured"));
        attributes.put("facility", Integer.toString(priority / 8));
        String process = matcher.group("process");
        if (!"-".equals(process)) {
            attributes.put("process", process);
        }
        String message = matcher.group("message");
        if (message == null || message.isBlank()) {
            message = "Syslog event " + matcher.group("messageId");
        }
        String messageId = matcher.group("messageId");

        return new ParsedLogLine(
                format(),
                ParserSupport.parseInstant(matcher.group("timestamp"), format()),
                ParserSupport.firstPresent(
                        matcher.group("host"), hints.source(), "source", format()),
                ParserSupport.firstPresent(
                        matcher.group("app"), hints.service(), "service", format()),
                severityFromPriority(priority),
                "-".equals(messageId) ? null : messageId,
                message,
                attributes.get("trace_id"),
                firstNonBlank(attributes.get("subject_id"), attributes.get("user_id")),
                attributes.get("event_id"),
                attributes);
    }

    private static Map<String, String> structuredAttributes(String structured) {
        if ("-".equals(structured)) {
            return new LinkedHashMap<>();
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        Matcher matcher = STRUCTURED_VALUE.matcher(structured);
        while (matcher.find()) {
            attributes.put(matcher.group("key"), matcher.group("value"));
        }
        return attributes;
    }

    private static EventSeverity severityFromPriority(int priority) {
        return switch (priority % 8) {
            case 0, 1, 2 -> EventSeverity.FATAL;
            case 3 -> EventSeverity.ERROR;
            case 4 -> EventSeverity.WARN;
            case 7 -> EventSeverity.DEBUG;
            default -> EventSeverity.INFO;
        };
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }
}
