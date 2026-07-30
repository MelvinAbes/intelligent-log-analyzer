package dev.loganalysis.event.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LogEventDraftTest {

    @Test
    void normalizesRequiredTextAndCopiesAttributes() {
        var mutableAttributes = new java.util.LinkedHashMap<>(Map.of("region", "eu-central"));

        var draft =
                new LogEventDraft(
                        Instant.parse("2026-07-30T10:15:30Z"),
                        " device-17 ",
                        " gateway ",
                        EventSeverity.WARN,
                        "device.offline",
                        " Connection lost ",
                        null,
                        null,
                        null,
                        mutableAttributes,
                        null);
        mutableAttributes.put("region", "changed");

        assertThat(draft.source()).isEqualTo("device-17");
        assertThat(draft.service()).isEqualTo("gateway");
        assertThat(draft.message()).isEqualTo("Connection lost");
        assertThat(draft.attributes()).containsEntry("region", "eu-central");
    }

    @Test
    void rejectsBlankRequiredFields() {
        assertThatThrownBy(
                        () ->
                                new LogEventDraft(
                                        Instant.now(),
                                        "source",
                                        " ",
                                        EventSeverity.INFO,
                                        null,
                                        "message",
                                        null,
                                        null,
                                        null,
                                        Map.of(),
                                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("service");
    }
}
