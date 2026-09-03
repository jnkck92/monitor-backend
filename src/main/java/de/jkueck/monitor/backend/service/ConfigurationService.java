package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class ConfigurationService {

    private final ConfigurationProperties properties;

    private final ObjectMapper yamlMapper;

    private final Map<String, Configuration> configCache = new ConcurrentHashMap<>();

    public ConfigurationService(
            ConfigurationProperties properties,
            @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper
    ) {
        this.properties = properties;
        this.yamlMapper = yamlMapper;
    }

    private Configuration loadConfigForTenant(String tenant) {
        Path path = Path.of(properties.path(), tenant, "instance-config.yaml");
        if (!Files.exists(path)) {
            throw new IllegalStateException("Configuration file does not exist for tenant: " + tenant + " at " + path);
        }
        try {
            Configuration configuration = yamlMapper.readValue(path.toFile(), Configuration.class);
            log.info("Configuration loaded for tenant {} – vehicles: {}, ruleGroups: {}",
                    tenant, configuration.vehicles().size(), configuration.ruleGroups().size());
            return configuration;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load configuration for tenant: " + tenant, e);
        }
    }

    public Configuration getConfigForTenant(String tenant) {
        return configCache.computeIfAbsent(tenant, this::loadConfigForTenant);
    }

    public void reloadTenant(String tenant) {
        configCache.remove(tenant);
        getConfigForTenant(tenant);
    }

    public void reloadAll() {
        configCache.clear();
        getKnownTenants().forEach(this::getConfigForTenant);
    }

    /**     * Einzige Quelle der Wahrheit für die Liste der bekannten Tenants.     * Wird von MonitorPollingService, DiveraConnectionValidator und     * ConfigurationService selbst (reloadAll) genutzt.     */
    public List<String> getKnownTenants() {
        return properties.tenants();
    }

}