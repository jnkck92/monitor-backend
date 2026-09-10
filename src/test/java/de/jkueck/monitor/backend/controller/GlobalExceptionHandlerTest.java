package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.exception.DiveraApiException;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void unknownTenantMapsTo404() {
        ProblemDetail problem = handler.handleUnknownTenant(new UnknownTenantException("foo"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
    }

    @Test
    void missingCredentialsMapsTo503() {
        ProblemDetail problem = handler.handleMissingCredentials(new MissingApiCredentialsException("foo"));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
    }

    @Test
    void diveraApiExceptionMapsTo502() {
        ProblemDetail problem = handler.handleDiveraApiException(new DiveraApiException("foo", "timeout", null));
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_GATEWAY.value());
    }
}