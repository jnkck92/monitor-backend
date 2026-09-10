package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ConfigurationService implements ConfigurationProvider {

    private final ConfigurationProperties properties;

    private final ObjectMapper yamlMapper;

    private final Map<String, Configuration> configCache = new ConcurrentHashMap<>();

    public ConfigurationService(ConfigurationProperties properties, @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper) {
        this.properties = properties;
        this.yamlMapper = yamlMapper;
    }

    private Configuration loadConfigForTenant(String tenant) {
        checkForKnownTenant(tenant);
        Path path = Path.of(properties.path(), tenant, "instance-config.yaml");
        if (!Files.exists(path)) {
            throw new UnknownTenantException(tenant, path);
        }
        try {
            Configuration configuration = yamlMapper.readValue(path.toFile(), Configuration.class);
            log.info("Configuration loaded for tenant {} – vehicles: {}, ruleGroups: {}",
                    tenant, configuration.vehicles().size(), configuration.ruleGroups().size());
            return configuration;
        } catch (JacksonIOException e) {
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' could not be read (I/O error): " + e.getMessage(), e);
        } catch (DatabindException e) {
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' has an invalid structure (field/type mismatch): " + e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' contains invalid YAML syntax: " + e.getMessage(), e);
        }
    }

    private void checkForKnownTenant(String tenant) {
        if (!properties.tenants().contains(tenant)) {
            throw new UnknownTenantException(tenant);
        }
    }

    @Override
    public Configuration getConfigForTenant(String tenant) {
        return configCache.computeIfAbsent(tenant, this::loadConfigForTenant);
    }

    @Override
    public void reloadTenant(String tenant) {
        configCache.remove(tenant);
        getConfigForTenant(tenant);
    }

    @Override
    public void reloadAll() {
        configCache.clear();
        getKnownTenants().forEach(this::getConfigForTenant);
    }

    @Override
    public List<String> getKnownTenants() {
        return properties.tenants();
    }

}