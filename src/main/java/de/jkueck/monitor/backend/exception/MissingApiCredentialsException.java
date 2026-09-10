package de.jkueck.monitor.backend.exception;

public class MissingApiCredentialsException extends RuntimeException {

    public MissingApiCredentialsException(String tenant) {
        super("No Divera accessKey configured for tenant: " + tenant);
    }

}