package de.jkueck.monitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "divera")
public record DiveraProperties(

        String baseUrl,

        long pollIntervalMs,

        @DefaultValue("5s") Duration connectTimeout,

        @DefaultValue("10s") Duration readTimeout

) {
}