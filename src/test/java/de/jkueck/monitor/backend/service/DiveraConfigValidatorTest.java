package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiveraConfigValidatorTest {

    @Test
    @DisplayName("wirft nichts wenn accessKey gesetzt ist")
    void doesNotThrowWhenAccessKeyPresent() {
        DiveraConfig config = new DiveraConfig("test-key", "https://example.com");

        assertThatCode(() -> DiveraConfigValidator.requireAccessKey("musterstadt", config))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("wirft MissingApiCredentialsException wenn config null ist")
    void throwsWhenConfigIsNull() {
        assertThatThrownBy(() -> DiveraConfigValidator.requireAccessKey("musterstadt", null))
                .isInstanceOf(MissingApiCredentialsException.class)
                .hasMessageContaining("musterstadt");
    }

    @Test
    @DisplayName("wirft MissingApiCredentialsException wenn accessKey null ist")
    void throwsWhenAccessKeyIsNull() {
        DiveraConfig config = new DiveraConfig(null, "https://example.com");

        assertThatThrownBy(() -> DiveraConfigValidator.requireAccessKey("musterstadt", config))
                .isInstanceOf(MissingApiCredentialsException.class);
    }

    @Test
    @DisplayName("wirft MissingApiCredentialsException wenn accessKey blank ist")
    void throwsWhenAccessKeyIsBlank() {
        DiveraConfig config = new DiveraConfig("   ", "https://example.com");

        assertThatThrownBy(() -> DiveraConfigValidator.requireAccessKey("musterstadt", config))
                .isInstanceOf(MissingApiCredentialsException.class);
    }
}