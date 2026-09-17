package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import tools.jackson.core.exc.JacksonIOException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
public class ConfigurationService implements ConfigurationProvider {

    private final ConfigurationProperties properties;

    private final ObjectMapper yamlMapper;

    private final MeterRegistry meterRegistry;

    private final Map<String, Configuration> configCache = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastReloadTimestamp = new ConcurrentHashMap<>();

    public ConfigurationService(ConfigurationProperties properties, @Qualifier("yamlObjectMapper") ObjectMapper yamlMapper, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.yamlMapper = yamlMapper;
        this.meterRegistry = meterRegistry;
    }

    private Configuration loadConfigForTenant(String tenant) {
        checkForKnownTenant(tenant);
        Path path = Path.of(properties.path(), tenant, "instance-config.yaml");
        if (!Files.exists(path)) {
            recordReload(tenant, "failure");
            throw new UnknownTenantException(tenant, path);
        }
        try {
            Configuration configuration = yamlMapper.readValue(path.toFile(), Configuration.class);
            log.info("Configuration loaded for tenant {} – vehicles: {}, ruleGroups: {}",
                    tenant, configuration.vehicles().size(), configuration.ruleGroups().size());
            recordReload(tenant, "success");
            return configuration;
        } catch (JacksonIOException e) {
            recordReload(tenant, "failure");
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' could not be read (I/O error): " + e.getMessage(), e);
        } catch (DatabindException e) {
            recordReload(tenant, "failure");
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' has an invalid structure (field/type mismatch): " + e.getMessage(), e);
        } catch (RuntimeException e) {
            recordReload(tenant, "failure");
            throw new ConfigurationLoadException("Configuration file for tenant '" + tenant + "' contains invalid YAML syntax: " + e.getMessage(), e);
        }
    }

    private void recordReload(String tenant, String result) {
        meterRegistry.counter("monitor.config.reload.total", "tenant", tenant, "result", result).increment();
        if ("success".equals(result)) {
            lastReloadTimestamp.computeIfAbsent(tenant, t -> {
                AtomicLong ref = new AtomicLong();
                meterRegistry.gauge("monitor.config.last_reload_timestamp", Tags.of("tenant", tenant), ref, AtomicLong::get);
                return ref;
            }).set(Instant.now().getEpochSecond());
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