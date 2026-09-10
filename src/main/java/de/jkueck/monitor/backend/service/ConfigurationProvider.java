package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;

import java.util.List;

public interface ConfigurationProvider {

    Configuration getConfigForTenant(String tenant);

    void reloadTenant(String tenant);

    void reloadAll();

    List<String> getKnownTenants();

}
