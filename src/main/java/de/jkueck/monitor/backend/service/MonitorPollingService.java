package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.configuration.*;
import de.jkueck.monitor.backend.dto.response.AlarmWebResponse;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorPollingService {

    private final DiveraClient client;

    private final ConfigurationService configService;

    @Getter
    private final AtomicReference<MonitorWebResponse> currentState = new AtomicReference<>(new MonitorWebResponse("DEFAULT", "STANDBY", List.of(), List.of(), null, null, null));

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

            MonitorWebResponse newState = buildState(alarmResponse, statusResponse.data(), configuration);
            currentState.set(newState);

            log.info("Poll ok – mode: {}, persons: {}, vehicles: {}", newState.mode(), newState.persons().size(), newState.vehicles().size());
        } catch (Exception e) {
            log.error("Error during poll: {}", e.getMessage(), e);
            MonitorWebResponse old = currentState.get();
            currentState.set(new MonitorWebResponse(old.departmentName(), old.mode(), old.persons(), old.vehicles(), old.alarm(), old.lastUpdate(), e.getMessage()));
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

        List<UnitWebResponse> alarmedVehicles = allVehicleUnits.stream()
                .map(v -> new UnitWebResponse(
                        v.id(),
                        v.name(),
                        v.callSign(),
                        rule.vehicleOrder().contains(v.id()),
                        v.radioStatus()
                ))
                .toList();

        AlarmWebResponse alarmInfo = new AlarmWebResponse(activeAlarm.get().title(), activeAlarm.get().address(), rule.label(), ruleGroup.color());

        return new MonitorWebResponse(configuration.departmentName(),"ALARM", allPersonUnits, alarmedVehicles, alarmInfo, Instant.now(), null);
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
                .orElse(new Rule("Unbekannter Einsatz", List.of(), List.of()));
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