package dev.loganalysis.parsing;

import dev.loganalysis.event.domain.EventSeverity;
import dev.loganalysis.event.domain.LogFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AccessLogParser implements LogLineParser {

    private static final Pattern ACCESS_PATTERN =
            Pattern.compile(
                    "^(?<remote>\\S+)\\s+\\S+\\s+\\S+\\s+\\[(?<timestamp>[^]]+)]\\s+"
                            + "\"(?<method>[A-Z]+)\\s+(?<path>\\S+)\\s+(?<protocol>[^\"]+)\"\\s+"
                            + "(?<status>\\d{3})\\s+(?<bytes>\\S+)"
                            + "(?:\\s+\"(?<referer>[^\"]*)\"\\s+\"(?<agent>[^\"]*)\")?$");
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    @Override
    public LogFormat format() {
        return LogFormat.ACCESS;
    }

    @Override
    public boolean supports(String line) {
        return ACCESS_PATTERN.matcher(line).matches();
    }

    @Override
    public ParsedLogLine parse(String line, ParseHints hints) {
        Matcher matcher = ACCESS_PATTERN.matcher(line);
        if (!matcher.matches()) {
            throw new LogParsingException(
                    "invalid_access_log",
                    format(),
                    "Log line is not a supported access-log entry.");
        }
        int status = Integer.parseInt(matcher.group("status"));
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("method", matcher.group("method"));
        attributes.put("path", matcher.group("path"));
        attributes.put("protocol", matcher.group("protocol"));
        attributes.put("status", Integer.toString(status));
        attributes.put("bytes", matcher.group("bytes"));
        putIfPresent(attributes, "referer", matcher.group("referer"));
        putIfPresent(attributes, "user_agent", matcher.group("agent"));

        String service = ParserSupport.firstPresent(null, hints.service(), "service", format());
        String source =
                ParserSupport.firstPresent(
                        matcher.group("remote"), hints.source(), "source", format());
        String message =
                "%s %s returned %d"
                        .formatted(matcher.group("method"), matcher.group("path"), status);
        return new ParsedLogLine(
                format(),
                OffsetDateTime.parse(matcher.group("timestamp"), TIMESTAMP_FORMAT).toInstant(),
                source,
                service,
                severity(status),
                "http." + status,
                message,
                null,
                null,
                null,
                attributes);
    }

    private static EventSeverity severity(int status) {
        if (status >= 500) {
            return EventSeverity.ERROR;
        }
        if (status >= 400) {
            return EventSeverity.WARN;
        }
        return EventSeverity.INFO;
    }

    private static void putIfPresent(Map<String, String> attributes, String key, String value) {
        if (value != null && !value.isBlank() && !"-".equals(value)) {
            attributes.put(key, value);
        }
    }
}
