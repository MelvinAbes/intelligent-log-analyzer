package dev.loganalysis.parsing;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class SensitiveDataRedactor {

    public static final String REDACTED = "[REDACTED]";

    private static final Set<String> SENSITIVE_KEYS =
            Set.of(
                    "password",
                    "passwd",
                    "secret",
                    "token",
                    "access_token",
                    "api_key",
                    "apikey",
                    "authorization");
    private static final Pattern KEY_VALUE_SECRET =
            Pattern.compile(
                    "(?i)\\b(password|passwd|secret|token|access[_-]?token|api[_-]?key|authorization)"
                            + "\\s*[:=]\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s,;]+)");
    private static final Pattern BEARER_SECRET =
            Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/=-]+");

    public String redact(String value) {
        if (value == null) {
            return null;
        }
        String keyValueRedacted = KEY_VALUE_SECRET.matcher(value).replaceAll("$1=" + REDACTED);
        return BEARER_SECRET.matcher(keyValueRedacted).replaceAll("Bearer " + REDACTED);
    }

    public Map<String, String> redact(Map<String, String> attributes) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        attributes.forEach(
                (key, value) -> {
                    String normalizedKey = key.toLowerCase(Locale.ROOT).replace('-', '_');
                    sanitized.put(
                            key, SENSITIVE_KEYS.contains(normalizedKey) ? REDACTED : redact(value));
                });
        return Map.copyOf(sanitized);
    }
}
