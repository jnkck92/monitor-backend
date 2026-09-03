package de.jkueck.monitor.backend.config;

import java.util.List;

@org.springframework.boot.context.properties.ConfigurationProperties(prefix = "configuration")
public record ConfigurationProperties(

        String path,

        List<String>tenants

) {}