package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.exception.DiveraApiException;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import de.jkueck.monitor.backend.service.ConfigurationProvider;
import de.jkueck.monitor.backend.service.ConfigurationService;
import de.jkueck.monitor.backend.service.DiveraConfigValidator;
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
    private final ConfigurationProvider configService;

    @EventListener(ApplicationReadyEvent.class)
    public void validateConnection() {
        log.info("Validating Divera API connections for all tenants...");

        for (String tenant : configService.getKnownTenants()) {
            validateTenant(tenant);
        }
    }

    private void validateTenant(String tenant) {

        Configuration config = configService.getConfigForTenant(tenant);
        DiveraConfig diveraConfig = config.divera();
        DiveraConfigValidator.requireAccessKey(tenant, diveraConfig);

        try {
            var response = client.pullAll(diveraConfig);
            if (response.success()) {
                log.info("[{}] Divera API connection validated successfully", tenant);
            } else {
                throw new DiveraApiException(tenant, "success=false", null);
            }
        } catch (MissingApiCredentialsException | DiveraApiException e) {
            throw e;
        } catch (Exception e) {
            throw new DiveraApiException(tenant, e.getMessage(), e);
        }

    }
}