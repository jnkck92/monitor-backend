package de.jkueck.monitor.backend.dto.configuration;

public record DiveraConfig(
        String accessKey,
        String baseUrl
) {

    public boolean hasAccessKey() {
        return accessKey != null && !accessKey.isBlank();
    }

}