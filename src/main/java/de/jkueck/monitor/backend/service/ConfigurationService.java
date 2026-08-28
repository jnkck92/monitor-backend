package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class ConfigurationService {

    private final ConfigurationProperties properties;

    private final ObjectMapper yamlMapper;

    private final AtomicReference<Configuration> currentConfig = new AtomicReference<>();

    public ConfigurationService(
            ConfigurationProperties properties,
            @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper
    ) {
        this.properties = properties;
        this.yamlMapper = yamlMapper;
    }

    @PostConstruct
    public void loadConfig() {
        Path path = Path.of(properties.path());

        if (!Files.exists(path)) {
            throw new IllegalStateException("Configuration file does not exist: " + path);
        }

        Configuration configuration = yamlMapper.readValue(path.toFile(), Configuration.class);
        currentConfig.set(configuration);
        log.info("Configuration loaded from {} – vehicles: {}, rulesGroups: {}", path, configuration.vehicles().size(), configuration.ruleGroups().size());
    }

    public void reload() {
        loadConfig();
    }

    public Configuration getConfig() {
        Configuration configuration = currentConfig.get();
        if (configuration == null) {
            throw new IllegalStateException("Configuration has not been loaded yet");
        }
        return configuration;
    }
}