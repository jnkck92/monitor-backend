package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.DiveraApiException;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnknownTenantException.class)
    public ProblemDetail handleUnknownTenant(UnknownTenantException ex) {
        log.warn("UnknownTenantException: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, "Unknown tenant", ex.getMessage());
    }

    @ExceptionHandler(MissingApiCredentialsException.class)
    public ProblemDetail handleMissingCredentials(MissingApiCredentialsException ex) {
        log.warn("MissingApiCredentialsException: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service not configured", ex.getMessage());
    }

    @ExceptionHandler(DiveraApiException.class)
    public ProblemDetail handleDiveraApiException(DiveraApiException ex) {
        log.error("DiveraApiException: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "Upstream API error", ex.getMessage());
    }

    @ExceptionHandler(ConfigurationLoadException.class)
    public ProblemDetail handleConfigurationLoadException(ConfigurationLoadException ex) {
        log.error("ConfigurationLoadException: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Configuration error", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service not ready", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException ex) {
        log.warn("MissingRequestHeaderException: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Missing header", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

}