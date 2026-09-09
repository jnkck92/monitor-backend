package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Rule;
import de.jkueck.monitor.backend.dto.configuration.RuleGroup;
import de.jkueck.monitor.backend.dto.configuration.Unit;
import de.jkueck.monitor.backend.dto.response.AlarmWebResponse;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonitorStateBuilder {

    private static final RuleGroup DEFAULT_RULE_GROUP =
            new RuleGroup("Unbekannter Einsatz", "Unbekannter Einsatz", "#999999", List.of());
    private static final Rule DEFAULT_RULE =
            new Rule("Unbekannter Einsatz", List.of(), List.of(), null, null, null);

    private final KeywordMatcher keywordMatcher;
    private final VehicleOrderBuilder vehicleOrderBuilder;
    private final UnitStatusEnricher statusEnricher;
    private final ActiveAlarmResolver activeAlarmResolver;
    private final Clock clock;

    public MonitorWebResponse build(DiveraResponse alarmResponse, List<VehicleStatus> liveStatuses, Configuration configuration) {
        List<UnitWebResponse> persons = enrichUnits(configuration.persons(), liveStatuses, configuration);
        List<UnitWebResponse> vehicles = enrichUnits(configuration.vehicles(), liveStatuses, configuration);

        return activeAlarmResolver.find(alarmResponse)
                .map(alarm -> buildAlarmState(alarm, vehicles, configuration, persons))
                .orElse(standbyState(configuration, persons, vehicles));
    }

    private MonitorWebResponse buildAlarmState(AlarmResponse alarm, List<UnitWebResponse> vehicles,
                                               Configuration configuration, List<UnitWebResponse> persons) {
        RuleGroup ruleGroup = keywordMatcher.matchRuleGroup(alarm, configuration).orElse(DEFAULT_RULE_GROUP);
        Rule rule = keywordMatcher.matchRule(alarm, ruleGroup).orElse(DEFAULT_RULE);

        List<UnitWebResponse> alarmedVehicles = vehicleOrderBuilder.buildOrderedList(vehicles, rule, configuration);
        List<UnitWebResponse> alarmedPersons = markAllAlerted(persons);
        AlarmTitleParser.TitleParts titleParts = AlarmTitleParser.parse(alarm.title());
        Instant alarmTimestamp = alarm.date() != null ? Instant.ofEpochSecond(alarm.date()) : null;
        AlarmWebResponse alarmInfo = new AlarmWebResponse(alarm.title(), titleParts.keyword(), titleParts.description(),
                alarm.address(), rule.label(), ruleGroup.color(), rule.hint(), alarmTimestamp);

        return new MonitorWebResponse(configuration.departmentName(), MonitorMode.ALARM.name(),
                alarmedPersons, alarmedVehicles, alarmInfo, Instant.now(clock), null);
    }

    private List<UnitWebResponse> markAllAlerted(List<UnitWebResponse> persons) {
        return persons.stream()
                .map(p -> new UnitWebResponse(p.id(), p.name(), p.callSign(), true, p.radioStatus(), p.ownVehicle()))
                .toList();
    }

    private MonitorWebResponse standbyState(Configuration configuration,
                                            List<UnitWebResponse> persons, List<UnitWebResponse> vehicles) {
        return new MonitorWebResponse(configuration.departmentName(), MonitorMode.STANDBY.name(),
                persons, vehicles, null, Instant.now(clock), null);
    }

    private List<UnitWebResponse> enrichUnits(List<Unit> units, List<VehicleStatus> liveStatuses,
                                              Configuration configuration) {
        return units.stream()
                .map(u -> statusEnricher.enrich(u, liveStatuses, configuration))
                .toList();
    }
}