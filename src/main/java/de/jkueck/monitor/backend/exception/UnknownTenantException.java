package de.jkueck.monitor.backend.exception;

import java.nio.file.Path;

public class UnknownTenantException extends RuntimeException {

    public UnknownTenantException(String tenant) {
        super("Unknown tenant: " + tenant);
    }

    public UnknownTenantException(String tenant, Path path) {
        super("Configuration file does not exist for tenant: " + tenant + " at " + path);
    }


}
