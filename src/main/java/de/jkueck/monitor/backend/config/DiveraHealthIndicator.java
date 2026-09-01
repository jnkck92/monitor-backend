package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.service.MonitorPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DiveraHealthIndicator implements HealthIndicator {

    private final MonitorPollingService pollingService;

    @Override
    public Health health() {
        var state = pollingService.getCurrentState().get();

        if (state.error() != null) {
            return Health.down()
                    .withDetail("mode", state.mode())
                    .withDetail("error", state.error())
                    .build();
        }

        return Health.up()
                .withDetail("mode", state.mode())
                .withDetail("department", state.departmentName())
                .withDetail("lastUpdate", state.lastUpdate())
                .build();
    }
}