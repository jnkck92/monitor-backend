package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.*;
import de.jkueck.monitor.backend.dto.response.AlarmWebResponse;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import de.jkueck.monitor.backend.event.MonitorStateChangedEvent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorPollingService {

    private static final Logger responseLogger = LoggerFactory.getLogger("divera.response");

    private final DiveraClient client;

    private final ConfigurationService configService;

    private final ObjectMapper jsonObjectMapper;

    private final ApplicationEventPublisher eventPublisher;

    @Getter
    private final AtomicReference<MonitorWebResponse> currentState = new AtomicReference<>(new MonitorWebResponse("DEFAULT", "STANDBY", List.of(), List.of(), null, null, null));

    private final AtomicReference<Long> lastLoggedAlarmId = new AtomicReference<>(null);

    @PostConstruct
    public void initialPoll() {
        poll();
    }

    @Scheduled(fixedRateString = "${divera.poll-interval-ms}")
    public void poll() {
        try {
            Configuration configuration = configService.getConfig();

            DiveraResponse alarmResponse = client.pullAll();
            VehicleStatusGroupResponse statusResponse = client.pullVehicleStatus();

            logIfChanged(alarmResponse, statusResponse);

            MonitorWebResponse newState = buildState(alarmResponse, statusResponse.data(), configuration);
            MonitorWebResponse oldState = currentState.getAndSet(newState);

            if (hasChanged(oldState, newState)) {
                log.info("State changed: {} → {}", oldState.mode(), newState.mode());
                eventPublisher.publishEvent(new MonitorStateChangedEvent(this, newState));
            }

            currentState.set(newState);

            log.debug("Poll ok – mode: {}, persons: {}, vehicles: {}", newState.mode(), newState.persons().size(), newState.vehicles().size());
        } catch (Exception e) {
            log.error("Error during poll: {}", e.getMessage(), e);
            MonitorWebResponse old = currentState.get();
            currentState.set(new MonitorWebResponse(old.departmentName(), old.mode(), old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
        }
    }

    private boolean hasChanged(MonitorWebResponse oldState, MonitorWebResponse newState) {
        // Mode-Wechsel (STANDBY ↔ ALARM)
        if (!oldState.mode().equals(newState.mode())) return true;

        // Alarm-Inhalt geändert
        if (newState.alarm() != null && oldState.alarm() != null
                && !newState.alarm().equals(oldState.alarm())) return true;

        // Vehicle-Status geändert
        if (!oldState.vehicles().equals(newState.vehicles())) return true;

        return false;
    }

    private void logIfChanged(DiveraResponse alarmResponse, VehicleStatusGroupResponse statusResponse) {
        Optional<AlarmResponse> activeAlarm = findActiveAlarm(alarmResponse);

        Long currentAlarmId = activeAlarm.map(AlarmResponse::id).orElse(null);
        Long previousAlarmId = lastLoggedAlarmId.get();

        if (!java.util.Objects.equals(currentAlarmId, previousAlarmId)) {
            lastLoggedAlarmId.set(currentAlarmId);

            if (currentAlarmId != null) {
                try {
                    responseLogger.info("ALARM DETECTED [id={}]: alarms={}", currentAlarmId, jsonObjectMapper.writeValueAsString(alarmResponse));
                } catch (Exception e) {
                    log.error("Failed to serialize response for logging", e);
                }
            } else {
                responseLogger.info("ALARM ENDED (previous id={})", previousAlarmId);
            }
        }
    }

    private MonitorWebResponse buildState(DiveraResponse alarmResponse, List<VehicleStatus> liveStatuses, Configuration configuration) {
        Optional<AlarmResponse> activeAlarm = findActiveAlarm(alarmResponse);

        List<UnitWebResponse> allPersonUnits = configuration.persons().stream()
                .map(v -> enrichWithStatus(v, liveStatuses, configuration))
                .toList();

        List<UnitWebResponse> allVehicleUnits = configuration.vehicles().stream()
                .map(v -> enrichWithStatus(v, liveStatuses, configuration))
                .toList();

        if (activeAlarm.isEmpty()) {
            return new MonitorWebResponse(configuration.departmentName(), "STANDBY", allPersonUnits, allVehicleUnits, null, Instant.now(), null);
        }

        RuleGroup ruleGroup = matchRuleGroup(activeAlarm.get(), configuration);

        Rule rule = matchRule(activeAlarm.get(), ruleGroup);

        List<UnitWebResponse> alarmedVehicles = buildOrderedVehicleList(allVehicleUnits, rule, configuration);

        AlarmWebResponse alarmInfo = new AlarmWebResponse(activeAlarm.get().title(), activeAlarm.get().address(), rule.label(), ruleGroup.color());

        return new MonitorWebResponse(configuration.departmentName(), "ALARM", allPersonUnits, alarmedVehicles, alarmInfo, Instant.now(), null);
    }

    private Optional<AlarmResponse> findActiveAlarm(DiveraResponse response) {
        if (response.data() == null || response.data().items() == null) {
            return Optional.empty();
        }
        return response.data().items().values().stream()
                .filter(a -> a.closed() == null || !a.closed())
                .findFirst();
    }

    private RuleGroup matchRuleGroup(AlarmResponse alarm, Configuration configuration) {
        return configuration.ruleGroups().stream()
                .filter(group -> group.rules().stream()
                        .anyMatch(rule -> rule.keywords().stream()
                                .anyMatch(kw -> alarm.title() != null && alarm.title().contains(kw))))
                .findFirst()
                .orElse(new RuleGroup("Unbekannter Einsatz", "Unbekannter Einsatz", "#999999", List.of()));
    }

    private Rule matchRule(AlarmResponse alarm, RuleGroup ruleGroup) {
        return ruleGroup.rules().stream()
                .filter(rule -> rule.keywords().stream()
                        .anyMatch(kw -> alarm.title() != null && alarm.title().contains(kw)))
                .findFirst()
                .orElse(new Rule("Unbekannter Einsatz", List.of(), List.of(), null));
    }

    private List<UnitWebResponse> buildOrderedVehicleList(List<UnitWebResponse> allVehicleUnits, Rule rule, Configuration configuration) {
        List<UnitWebResponse> orderedAlerted = rule.vehicleOrder().stream()
                .map(id -> allVehicleUnits.stream()
                        .filter(v -> v.id().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(v -> new UnitWebResponse(v.id(), v.name(), v.callSign(), true, v.radioStatus()))
                .toList();

        List<String> nonAlertedOrder = (rule.remainingOrder() != null && !rule.remainingOrder().isEmpty())
                ? rule.remainingOrder()
                : configuration.defaultOrder();

        List<UnitWebResponse> orderedNonAlerted = nonAlertedOrder.stream()
                .filter(id -> !rule.vehicleOrder().contains(id))
                .map(id -> allVehicleUnits.stream()
                        .filter(v -> v.id().equals(id))
                        .findFirst()
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .map(v -> new UnitWebResponse(v.id(), v.name(), v.callSign(), false, v.radioStatus()))
                .toList();

        List<UnitWebResponse> result = new java.util.ArrayList<>(orderedAlerted);
        result.addAll(orderedNonAlerted);
        return result;
    }

    private UnitWebResponse enrichWithStatus(Unit unit, List<VehicleStatus> liveStatuses, Configuration configuration) {
        VehicleStatus live = liveStatuses.stream()
                .filter(s -> s.id().equals(unit.diveraId()))
                .findFirst()
                .orElse(null);

        if (live == null) {
            return new UnitWebResponse(
                    unit.id(),
                    unit.shortName(),
                    unit.ric(),
                    false,
                    new RadioStatusWebResponse("UNBEKANNT", "#cccccc")
            );
        }

        String statusKey = String.valueOf(live.fmsstatus());

        Status status = getStatus(configuration, statusKey);

        return new UnitWebResponse(
                unit.id(),
                unit.shortName(),
                unit.ric(),
                true,
                new RadioStatusWebResponse(status.label(), status.color())
        );
    }

    private Status getStatus(Configuration configuration, String key) {
        return configuration.statuses().getOrDefault(key, new Status("Unbekannt", "#cccccc"));

    }

}