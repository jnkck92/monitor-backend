package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.*;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MonitorStateBuilderTest {

    private final KeywordMatcher ruleResolver = new KeywordMatcher();
    private final VehicleOrderBuilder vehicleOrderBuilder = new VehicleOrderBuilder();
    private final UnitStatusEnricher statusEnricher = new UnitStatusEnricher();
    private final ActiveAlarmResolver activeAlarmResolver = new ActiveAlarmResolver();
    private final MonitorStateBuilder stateBuilder = new MonitorStateBuilder(ruleResolver, vehicleOrderBuilder, statusEnricher, activeAlarmResolver, Clock.fixed(Instant.parse("2026-09-01T12:00:00Z"), ZoneId.of("UTC")));

    private Configuration defaultConfig() {
        Unit vehicle = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 100L);
        Unit person = new Unit("p1", "Max", "Max", "person", "P1", 200L);
        Rule rule = new Rule("Zimmerbrand", List.of("B2"), List.of("v1"), null, null, "Atemschutz bereitstellen");
        RuleGroup group = new RuleGroup("Brand", "Brandeinsatz", "#ff0000", List.of(rule));
        return new Configuration("TestFW", null, List.of(person), List.of(vehicle),
                List.of("v1"), null,
                Map.of("2", new Status("Status 2", "#00ff00")),
                List.of(group));
    }

    private DiveraResponse noAlarmResponse() {
        return new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
    }

    private DiveraResponse activeAlarmResponse(String title) {
        AlarmResponse alarm = new AlarmResponse(1L, title, null, "Musterstr. 1", null, false, false);
        return new DiveraResponse(true, new DiveraResponse.Data(Map.of("1", alarm)));
    }

    @Test
    void buildReturnsStandbyWhenNoAlarm() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(noAlarmResponse(), live, defaultConfig());

        assertThat(result.mode()).isEqualTo("STANDBY");
        assertThat(result.alarm()).isNull();
        assertThat(result.departmentName()).isEqualTo("TestFW");
        assertThat(result.vehicles()).hasSize(1);
        assertThat(result.persons()).hasSize(1);
    }

    @Test
    void buildReturnsAlarmWhenActiveAlarm() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(activeAlarmResponse("B2 Zimmerbrand"), live, defaultConfig());

        assertThat(result.mode()).isEqualTo("ALARM");
        assertThat(result.alarm()).isNotNull();
        assertThat(result.alarm().title()).isEqualTo("B2 Zimmerbrand");
        assertThat(result.alarm().label()).isEqualTo("Zimmerbrand");
        assertThat(result.alarm().color()).isEqualTo("#ff0000");
        assertThat(result.alarm().address()).isEqualTo("Musterstr. 1");
    }

    @Test
    void buildReturnsAlarmWithDefaultRuleWhenNoKeywordMatch() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(activeAlarmResponse("XYZ Unbekannt"), live, defaultConfig());

        assertThat(result.mode()).isEqualTo("ALARM");
        assertThat(result.alarm().label()).isEqualTo("Unbekannter Einsatz");
        assertThat(result.alarm().color()).isEqualTo("#999999");
    }

    @Test
    void buildHandlesNullData() {
        DiveraResponse response = new DiveraResponse(true, null);
        List<VehicleStatus> live = List.of();

        MonitorWebResponse result = stateBuilder.build(response, live, defaultConfig());

        assertThat(result.mode()).isEqualTo("STANDBY");
    }

    @Test
    void buildHandlesClosedAlarm() {
        AlarmResponse closed = new AlarmResponse(1L, "B2 Brand", null, null, null, true, false);
        DiveraResponse response = new DiveraResponse(true, new DiveraResponse.Data(Map.of("1", closed)));
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(response, live, defaultConfig());

        assertThat(result.mode()).isEqualTo("STANDBY");
    }

    @Test
    void findActiveAlarmReturnsEmptyForNullItems() {
        DiveraResponse response = new DiveraResponse(true, new DiveraResponse.Data(null));

        assertThat(activeAlarmResolver.find(response)).isEmpty();
    }

    @Test
    void buildReturnsHintFromMatchedRule() {
        Unit vehicle = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 100L);
        Rule rule = new Rule("Zimmerbrand", List.of("B2"), List.of("v1"), null, null, "Atemschutz bereitstellen");
        RuleGroup group = new RuleGroup("Brand", "Brandeinsatz", "#ff0000", List.of(rule));
        Configuration config = new Configuration("TestFW", null, List.of(), List.of(vehicle),
                List.of("v1"), null,
                Map.of("2", new Status("Status 2", "#00ff00")),
                List.of(group));
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(activeAlarmResponse("B2 Zimmerbrand"), live, config);

        assertThat(result.alarm().hint()).isEqualTo("Atemschutz bereitstellen");
    }

    @Test
    void buildReturnsNullHintWhenRuleHasNoHint() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(activeAlarmResponse("B2 Zimmerbrand"), live, defaultConfig());

        assertThat(result.mode()).isEqualTo("ALARM");
        assertThat(result.alarm().hint()).isEqualTo("Atemschutz bereitstellen");
    }

    @Test
    void buildReturnsNullHintWhenNoRuleMatches() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(activeAlarmResponse("XYZ Unbekannt"), live, defaultConfig());

        assertThat(result.alarm().hint()).isNull();
    }

    @Test
    void buildReturnsNullHintInStandby() {
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));

        MonitorWebResponse result = stateBuilder.build(noAlarmResponse(), live, defaultConfig());

        assertThat(result.alarm()).isNull();
    }

}