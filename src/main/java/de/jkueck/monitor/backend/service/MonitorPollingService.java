package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MonitorPollingService {

    private final DiveraClient client;

    private final ConfigurationProvider configService;

    private final MonitorStateBuilder stateBuilder;

    private final DiveraResponseLogger responseLogger;

    private final Timer pollTimer;

    private final Counter pollErrorCounter;

    private final Counter stateChangeCounter;

    private final OwnVehicleMarker ownVehicleMarker;

    private final TenantStateStore stateStore;

    public MonitorPollingService(DiveraClient client,
                                 ConfigurationProvider configService,
                                 MonitorStateBuilder stateBuilder,
                                 DiveraResponseLogger responseLogger,
                                 MeterRegistry meterRegistry,
                                 OwnVehicleMarker ownVehicleMarker,
                                 TenantStateStore stateStore) {
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
        this.ownVehicleMarker = ownVehicleMarker;
        this.stateStore = stateStore;
    }

    @PostConstruct
    public void initialPoll() {
        poll();
    }

    public MonitorWebResponse getCurrentState(String tenant) {
        return stateStore.get(tenant);
    }

    public MonitorWebResponse getCurrentState(String tenant, String ownVehicleId) {
        return ownVehicleMarker.mark(getCurrentState(tenant), ownVehicleId);
    }

    public Map<String, MonitorWebResponse> getAllStates() {
        return stateStore.getAll();
    }

    @Scheduled(fixedRateString = "${divera.poll-interval-ms}")
    public void poll() {
        for (String tenant : configService.getKnownTenants()) {
            try {
                pollTenant(tenant);
            } catch (RuntimeException e) {
                pollErrorCounter.increment();
                log.error("[{}] Unerwarteter Fehler beim Poll - vermutlich ein Bug, bitte prüfen: {}", tenant, e.getMessage(), e);
            }
        }
    }

    private void pollTenant(String tenant) {
        pollTimer.record(() -> {
            try {
                Configuration config = configService.getConfigForTenant(tenant);
                DiveraConfig diveraConfig = config.divera();
                DiveraConfigValidator.requireAccessKey(tenant, diveraConfig);

                DiveraResponse alarmResponse = client.pullAll(diveraConfig);
                VehicleStatusGroupResponse statusResponse = client.pullVehicleStatus(diveraConfig);

                responseLogger.logIfChanged(alarmResponse);

                MonitorWebResponse newState = stateBuilder.build(alarmResponse, statusResponse.data(), config);
                MonitorWebResponse oldState = stateStore.getOrInitial(tenant);
                stateStore.put(tenant, newState);

                if (!oldState.mode().equals(newState.mode())) {
                    log.info("[{}] State changed: {} → {}", tenant, oldState.mode(), newState.mode());
                    stateChangeCounter.increment();
                }
            } catch (UnknownTenantException | ConfigurationLoadException | MissingApiCredentialsException | RestClientException e) {
                log.warn("[{}] Poll fehlgeschlagen (bekannter Fehler): {}", tenant, e.getMessage());
                recordPollError(tenant, e.getMessage());
            }
        });
    }

    private void recordPollError(String tenant, String message) {
        pollErrorCounter.increment();
        MonitorWebResponse old = stateStore.getOrInitial(tenant);
        stateStore.put(tenant, new MonitorWebResponse(old.departmentName(), old.mode(),
                old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), message));
    }

}