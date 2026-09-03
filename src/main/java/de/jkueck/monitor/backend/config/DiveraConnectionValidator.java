package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("live")
@RequiredArgsConstructor
public class DiveraConnectionValidator {

    private final DiveraClient client;
    private final ConfigurationService configService;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConnection() {
        log.info("Validating Divera API connections for all tenants...");

        for (String tenant : configService.getKnownTenants()) {
            validateTenant(tenant);
        }
    }

    private void validateTenant(String tenant) {
        try {
            Configuration config = configService.getConfigForTenant(tenant);
            DiveraConfig diveraConfig = config.divera();

            if (diveraConfig == null || diveraConfig.accessKey() == null || diveraConfig.accessKey().isBlank()) {
                throw new IllegalStateException("No Divera accessKey configured for tenant: " + tenant);
            }

            var response = client.pullAll(diveraConfig);
            if (response.success()) {
                log.info("[{}] Divera API connection validated successfully", tenant);
            } else {
                log.error("[{}] Divera API returned success=false — check the access key", tenant);
                throw new IllegalStateException("Divera API validation failed for tenant " + tenant + ": success=false");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("[{}] Failed to connect to Divera API: {}", tenant, e.getMessage());
            throw new IllegalStateException("Divera API validation failed for tenant " + tenant + ": " + e.getMessage(), e);
        }
    }
}