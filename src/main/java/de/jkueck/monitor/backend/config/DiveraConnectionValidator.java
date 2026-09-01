package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.client.DiveraClient;
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

    @EventListener(ApplicationReadyEvent.class)
    public void validateConnection() {
        log.info("Validating Divera API connection...");
        try {
            var response = client.pullAll();
            if (response.success()) {
                log.info("Divera API connection validated successfully");
            } else {
                log.error("Divera API returned success=false — check your access key");
                throw new IllegalStateException("Divera API validation failed: success=false");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to connect to Divera API: {}", e.getMessage());
            throw new IllegalStateException("Divera API validation failed: " + e.getMessage(), e);
        }
    }
}