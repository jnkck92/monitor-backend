package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

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

    private final Map<String, AtomicReference<MonitorWebResponse>> stateByTenant = new ConcurrentHashMap<>();

    public MonitorPollingService(DiveraClient client,
                                  ConfigurationService configService,
                                  MonitorStateBuilder stateBuilder,
                                  DiveraResponseLogger responseLogger,
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

    public MonitorWebResponse getCurrentState(String tenant) {
        AtomicReference<MonitorWebResponse> ref = stateByTenant.get(tenant);
        if (ref == null) {
            throw new IllegalStateException("No monitor state available yet for tenant: " + tenant);
        }
        return ref.get();
    }

    public Map<String, MonitorWebResponse> getAllStates() {
        return stateByTenant.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get()));
    }

    @Scheduled(fixedRateString = "${divera.poll-interval-ms}")
    public void poll() {
        for (String tenant : configService.getKnownTenants()) {
            pollTenant(tenant);
        }
    }

    private void pollTenant(String tenant) {
        pollTimer.record(() -> {
            try {
                Configuration config = configService.getConfigForTenant(tenant);
                DiveraConfig diveraConfig = config.divera();

                if (diveraConfig == null || diveraConfig.accessKey() == null || diveraConfig.accessKey().isBlank()) {
                    throw new IllegalStateException("No Divera accessKey configured for tenant: " + tenant);
                }

                DiveraResponse alarmResponse = client.pullAll(diveraConfig);
                VehicleStatusGroupResponse statusResponse = client.pullVehicleStatus(diveraConfig);

                responseLogger.logIfChanged(alarmResponse);

                MonitorWebResponse newState = stateBuilder.build(alarmResponse, statusResponse.data(), config);

                AtomicReference<MonitorWebResponse> ref = stateByTenant.computeIfAbsent(tenant,
                        t -> new AtomicReference<>(new MonitorWebResponse(
                                "DEFAULT", "STANDBY", List.of(), List.of(), null, null, null)));

                MonitorWebResponse oldState = ref.getAndSet(newState);

                if (!oldState.mode().equals(newState.mode())) {
                    log.info("[{}] State changed: {} → {}", tenant, oldState.mode(), newState.mode());
                    stateChangeCounter.increment();
                }
            } catch (Exception e) {
                log.error("[{}] Error during poll: {}", tenant, e.getMessage(), e);
                pollErrorCounter.increment();

                AtomicReference<MonitorWebResponse> ref = stateByTenant.computeIfAbsent(tenant,
                        t -> new AtomicReference<>(new MonitorWebResponse(
                                "DEFAULT", "STANDBY", List.of(), List.of(), null, null, null)));
                MonitorWebResponse old = ref.get();
                ref.set(new MonitorWebResponse(old.departmentName(), old.mode(),
                        old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
            }
        });
    }
}