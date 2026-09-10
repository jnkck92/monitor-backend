package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;

public final class DiveraConfigValidator {

    private DiveraConfigValidator() {
    }

    public static void requireAccessKey(String tenant, DiveraConfig diveraConfig) {
        if (diveraConfig == null || !diveraConfig.hasAccessKey()) {
            throw new MissingApiCredentialsException(tenant);
        }
    }
}