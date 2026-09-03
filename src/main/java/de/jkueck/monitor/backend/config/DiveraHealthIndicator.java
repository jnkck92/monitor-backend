package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DiveraHealthIndicator implements HealthIndicator {

    private final MonitorPollingService pollingService;

    @Override
    public Health health() {
        Map<String, MonitorWebResponse> states = pollingService.getAllStates();

        if (states.isEmpty()) {
            return Health.unknown()
                    .withDetail("reason", "No tenant has been polled yet")
                    .build();
        }

        boolean anyError = states.values().stream().anyMatch(s -> s.error() != null);
        Health.Builder builder = anyError ? Health.down() : Health.up();

        states.forEach((tenant, state) -> {
            if (state.error() != null) {
                builder.withDetail(tenant, Map.of("mode", state.mode(), "error", state.error()));
            } else {
                builder.withDetail(tenant, Map.of(
                        "mode", state.mode(),
                        "department", state.departmentName(),
                        "lastUpdate", state.lastUpdate()));
            }
        });

        return builder.build();
    }
}