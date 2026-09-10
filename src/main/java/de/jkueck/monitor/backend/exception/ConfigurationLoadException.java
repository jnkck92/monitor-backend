package de.jkueck.monitor.backend.exception;

public class ConfigurationLoadException extends  RuntimeException {

    public ConfigurationLoadException(String message, Throwable cause) {
        super(message, cause);
    }

}
