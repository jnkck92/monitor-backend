package de.jkueck.monitor.backend.config;

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "configuration")
public record ConfigurationProperties(

        String path

) {}