package de.jkueck.monitor.backend.exception;

public class DiveraApiException extends RuntimeException {

    public DiveraApiException(String tenant, String message, Throwable cause) {
        super("[" + tenant + "] Divera API error: " + message, cause);
    }

}