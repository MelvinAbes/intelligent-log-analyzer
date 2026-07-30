package dev.loganalysis.parsing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MessageFingerprintTest {

    @Test
    void canonicalizesVariableIdentifiersAddressesAndNumbers() {
        MessageFingerprint fingerprint = new MessageFingerprint();

        String first =
                fingerprint.create(
                        "Request 1942 for 203.0.113.7 failed with id "
                                + "123e4567-e89b-42d3-a456-426614174000");
        String second =
                fingerprint.create(
                        "Request 2088 for 198.51.100.9 failed with id "
                                + "2f1a6ab1-36c4-4a93-99b2-e59129f4793e");

        assertThat(first).isEqualTo(second).hasSize(64);
    }
}
