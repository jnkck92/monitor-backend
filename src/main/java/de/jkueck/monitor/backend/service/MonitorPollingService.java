package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class MonitorPollingService {

    private final DiveraClient client;
    private final ConfigurationService configService;
    private final MonitorStateBuilder stateBuilder;
    private final DiveraResponseLogger responseLogger;
    private final Timer pollTimer;
    private final Counter pollErrorCounter;
    private final Counter stateChangeCounter;

    @Getter
    private final AtomicReference<MonitorWebResponse> currentState =
            new AtomicReference<>(new MonitorWebResponse("DEFAULT", "STANDBY", List.of(), List.of(), null, null, null));

    public MonitorPollingService(DiveraClient client, ConfigurationService configService,
                                 MonitorStateBuilder stateBuilder, DiveraResponseLogger responseLogger,
                                 MeterRegistry meterRegistry) {
        this.client = client;
        this.configService = configService;
        this.stateBuilder = stateBuilder;
        this.responseLogger = responseLogger;
        this.pollTimer = Timer.builder("monitor.poll.duration")
                .description("Duration of a single Divera poll cycle")
                .register(meterRegistry);
        this.pollErrorCounter = Counter.builder("monitor.poll.errors")
                .description("Number of failed poll attempts")
                .register(meterRegistry);
        this.stateChangeCounter = Counter.builder("monitor.state.changes")
                .description("Number of state transitions (e.g. STANDBY → ALARM)")
                .register(meterRegistry);
    }

    @PostConstruct
    public void initialPoll() {
        poll();
    }

    @Scheduled(fixedRateString = "${divera.poll-interval-ms}")
    public void poll() {
        pollTimer.record(() -> {
            try {
                Configuration config = configService.getConfig();
                DiveraResponse alarmResponse = client.pullAll();
                VehicleStatusGroupResponse statusResponse = client.pullVehicleStatus();

                responseLogger.logIfChanged(alarmResponse);

                MonitorWebResponse newState = stateBuilder.build(alarmResponse, statusResponse.data(), config);
                MonitorWebResponse oldState = currentState.getAndSet(newState);

                if (!oldState.mode().equals(newState.mode())) {
                    log.info("State changed: {} → {}", oldState.mode(), newState.mode());
                    stateChangeCounter.increment();
                }
            } catch (Exception e) {
                log.error("Error during poll: {}", e.getMessage(), e);
                pollErrorCounter.increment();
                MonitorWebResponse old = currentState.get();
                currentState.set(new MonitorWebResponse(old.departmentName(), old.mode(),
                        old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
            }
        });
    }
}