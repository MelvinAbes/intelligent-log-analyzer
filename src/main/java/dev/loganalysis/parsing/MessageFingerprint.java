package dev.loganalysis.parsing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class MessageFingerprint {

    private static final Pattern UUID_PATTERN =
            Pattern.compile(
                    "(?i)\\b[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\b");
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)\\b0x[0-9a-f]+\\b");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public String create(String message) {
        String canonical = canonicalize(message);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of()
                    .formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is not available", error);
        }
    }

    String canonicalize(String message) {
        String canonical = message.toLowerCase(Locale.ROOT);
        canonical = UUID_PATTERN.matcher(canonical).replaceAll("<uuid>");
        canonical = IPV4_PATTERN.matcher(canonical).replaceAll("<ip>");
        canonical = HEX_PATTERN.matcher(canonical).replaceAll("<hex>");
        canonical = NUMBER_PATTERN.matcher(canonical).replaceAll("<number>");
        return WHITESPACE_PATTERN.matcher(canonical).replaceAll(" ").strip();
    }
}
