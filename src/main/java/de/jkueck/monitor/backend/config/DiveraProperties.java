package de.jkueck.monitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "divera")
public record DiveraProperties(
        String baseUrl,
        long pollIntervalMs
) {
}