package de.jkueck.monitor.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "geocoding")
public record GeocodingProperties(

        @DefaultValue("https://nominatim.openstreetmap.org") String baseUrl,

        @DefaultValue("monitor-backend/1.0 (kontakt@jkueck.de)") String userAgent,

        @DefaultValue("5s") Duration connectTimeout,

        @DefaultValue("5s") Duration readTimeout

) {
}