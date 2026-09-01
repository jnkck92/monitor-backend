package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorPollingService {

    private final DiveraClient client;

    private final ConfigurationService configService;

    private final MonitorStateBuilder stateBuilder;

    private final DiveraResponseLogger responseLogger;

    @Getter
    private final AtomicReference<MonitorWebResponse> currentState = new AtomicReference<>(new MonitorWebResponse("DEFAULT", "STANDBY", List.of(), List.of(), null, null, null));

    @PostConstruct
    public void initialPoll() {
        poll();
    }

    @Scheduled(fixedRateString = "${divera.poll-interval-ms}")
    public void poll() {
        try {
            Configuration config = configService.getConfig();
            DiveraResponse alarmResponse = client.pullAll();
            VehicleStatusGroupResponse statusResponse = client.pullVehicleStatus();

            responseLogger.logIfChanged(alarmResponse);

            MonitorWebResponse newState = stateBuilder.build(alarmResponse, statusResponse.data(), config);
            MonitorWebResponse oldState = currentState.getAndSet(newState);

            if (!oldState.mode().equals(newState.mode())) {
                log.info("State changed: {} → {}", oldState.mode(), newState.mode());
            }
        } catch (Exception e) {
            log.error("Error during poll: {}", e.getMessage(), e);
            MonitorWebResponse old = currentState.get();
            currentState.set(new MonitorWebResponse(old.departmentName(), old.mode(), old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
        }
    }
}