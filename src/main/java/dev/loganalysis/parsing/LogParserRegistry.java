package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.LogFormat;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LogParserRegistry {

    private final Map<LogFormat, LogLineParser> parsers;
    private final List<LogLineParser> detectionOrder;

    public LogParserRegistry(List<LogLineParser> parsers) {
        Map<LogFormat, LogLineParser> byFormat = new EnumMap<>(LogFormat.class);
        for (LogLineParser parser : parsers) {
            if (byFormat.put(parser.format(), parser) != null) {
                throw new IllegalStateException("duplicate parser for " + parser.format());
            }
        }
        this.parsers = Map.copyOf(byFormat);
        detectionOrder =
                List.of(
                        required(byFormat, LogFormat.JSON_LINES),
                        required(byFormat, LogFormat.SYSLOG),
                        required(byFormat, LogFormat.ACCESS),
                        required(byFormat, LogFormat.APPLICATION));
    }

    public ParsedLogLine parse(String line, LogFormat requestedFormat, ParseHints hints) {
        if (line == null || line.isBlank()) {
            throw new LogParsingException(
                    "blank_line", requestedFormat, "Blank log lines cannot be ingested.");
        }
        if (requestedFormat != LogFormat.AUTO) {
            return required(parsers, requestedFormat).parse(line, hints);
        }
        return detectionOrder.stream()
                .filter(parser -> parser.supports(line))
                .findFirst()
                .orElseThrow(
                        () ->
                                new LogParsingException(
                                        "unknown_format",
                                        LogFormat.AUTO,
                                        "Log format could not be detected."))
                .parse(line, hints);
    }

    private static LogLineParser required(Map<LogFormat, LogLineParser> parsers, LogFormat format) {
        LogLineParser parser = parsers.get(format);
        if (parser == null) {
            throw new IllegalStateException("missing parser for " + format);
        }
        return parser;
    }
}
