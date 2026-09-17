package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.MissingApiCredentialsException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private final MeterRegistry meterRegistry;

    private final OwnVehicleMarker ownVehicleMarker;

    private final TenantStateStore stateStore;

    private final Clock clock;

    private final Set<String> gaugesRegistered = ConcurrentHashMap.newKeySet();

    public MonitorPollingService(DiveraClient client,
                                 ConfigurationProvider configService,
                                 MonitorStateBuilder stateBuilder,
                                 DiveraResponseLogger responseLogger,
                                 MeterRegistry meterRegistry,
                                 OwnVehicleMarker ownVehicleMarker,
                                 TenantStateStore stateStore,
                                 Clock clock) {
        this.client = client;
        this.configService = configService;
        this.stateBuilder = stateBuilder;
        this.responseLogger = responseLogger;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
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
                recordUnexpectedError(tenant, e);
                log.error("[{}] Unerwarteter Fehler beim Poll - vermutlich ein Bug, bitte prüfen: {}", tenant, e.getMessage(), e);
            }
        }
    }

    private void pollTenant(String tenant) {
        registerGaugesIfAbsent(tenant);
        Timer pollTimer = meterRegistry.timer("monitor.poll.duration", "tenant", tenant);
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
                    meterRegistry.counter("monitor.state.changes", "tenant", tenant,
                            "from", oldState.mode(), "to", newState.mode()).increment();
                    trackAlarmDuration(tenant, newState);
                }
            } catch (UnknownTenantException | ConfigurationLoadException | MissingApiCredentialsException | RestClientException e) {
                log.warn("[{}] Poll fehlgeschlagen (bekannter Fehler): {}", tenant, e.getMessage());
                recordPollError(tenant, e);
            }
        });
    }

    private void trackAlarmDuration(String tenant, MonitorWebResponse newState) {
        if (MonitorMode.ALARM.name().equals(newState.mode())) {
            stateStore.markAlarmStart(tenant, Instant.now(clock));
        } else if (MonitorMode.STANDBY.name().equals(newState.mode())) {
            Instant start = stateStore.clearAlarmStart(tenant);
            if (start != null) {
                Duration duration = Duration.between(start, Instant.now(clock));
                meterRegistry.timer("monitor.alarm.duration", "tenant", tenant).record(duration);
            }
        }
    }

    private void registerGaugesIfAbsent(String tenant) {
        if (!gaugesRegistered.add(tenant)) {
            return;
        }
        Tags tags = Tags.of("tenant", tenant);
        meterRegistry.gauge("monitor.alarm.active", tags, stateStore,
                store -> MonitorMode.ALARM.name().equals(store.getOrInitial(tenant).mode()) ? 1d : 0d);
        meterRegistry.gauge("monitor.persons.alerted", tags, stateStore,
                store -> countAlerted(store.getOrInitial(tenant).persons()));
        meterRegistry.gauge("monitor.persons.total", tags, stateStore,
                store -> (double) store.getOrInitial(tenant).persons().size());
        meterRegistry.gauge("monitor.vehicles.alerted", tags, stateStore,
                store -> countAlerted(store.getOrInitial(tenant).vehicles()));
        meterRegistry.gauge("monitor.vehicles.total", tags, stateStore,
                store -> (double) store.getOrInitial(tenant).vehicles().size());
    }

    private static double countAlerted(List<UnitWebResponse> units) {
        return units.stream().filter(UnitWebResponse::alerted).count();
    }

    private void recordUnexpectedError(String tenant, Exception e) {
        meterRegistry.counter("monitor.poll.errors", "tenant", tenant, "type", e.getClass().getSimpleName()).increment();
    }

    private void recordPollError(String tenant, Exception e) {
        meterRegistry.counter("monitor.poll.errors", "tenant", tenant, "type", e.getClass().getSimpleName()).increment();
        MonitorWebResponse old = stateStore.getOrInitial(tenant);
        stateStore.put(tenant, new MonitorWebResponse(old.departmentName(), old.mode(),
                old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
    }

}