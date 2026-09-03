package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Rule;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VehicleOrderBuilderTest {

    private final VehicleOrderBuilder builder = new VehicleOrderBuilder();

    private static final RadioStatusWebResponse STATUS_2 = new RadioStatusWebResponse("Status 2", "#00ff00");

    private final List<UnitWebResponse> allVehicles = List.of(
            new UnitWebResponse("v1", "LF20", "FL-FW 11", false, STATUS_2),
            new UnitWebResponse("v2", "DLK", "FL-FW 12", false, STATUS_2),
            new UnitWebResponse("v3", "RW", "FL-FW 13", false, STATUS_2),
            new UnitWebResponse("v4", "MTW", "FL-FW 14", false, STATUS_2)
    );

    private Configuration configWithDefaultOrder(List<String> defaultOrder) {
        return new Configuration("TestFW", null, List.of(), List.of(), defaultOrder, null, Map.of(), List.of());
    }

    @Test
    void alertedVehiclesAppearFirstAndAreMarkedAlerted() {
        Rule rule = new Rule("Brand", List.of("B2"), List.of("v2", "v1"), null, null, null);
        Configuration config = configWithDefaultOrder(List.of("v1", "v2", "v3", "v4"));

        List<UnitWebResponse> result = builder.buildOrderedList(allVehicles, rule, config);

        assertThat(result.get(0).id()).isEqualTo("v2");
        assertThat(result.get(0).alerted()).isTrue();
        assertThat(result.get(1).id()).isEqualTo("v1");
        assertThat(result.get(1).alerted()).isTrue();
    }

    @Test
    void nonAlertedVehiclesFollowInDefaultOrder() {
        Rule rule = new Rule("Brand", List.of("B2"), List.of("v2"), null, null, null);
        Configuration config = configWithDefaultOrder(List.of("v1", "v2", "v3", "v4"));

        List<UnitWebResponse> result = builder.buildOrderedList(allVehicles, rule, config);

        // v2 ist alerted, Rest folgt in defaultOrder ohne v2
        assertThat(result).hasSize(4);
        assertThat(result.get(0).id()).isEqualTo("v2");
        assertThat(result.get(1).id()).isEqualTo("v1");
        assertThat(result.get(1).alerted()).isFalse();
        assertThat(result.get(2).id()).isEqualTo("v3");
        assertThat(result.get(3).id()).isEqualTo("v4");
    }

    @Test
    void usesRemainingOrderWhenProvided() {
        Rule rule = new Rule("Brand", List.of("B2"), List.of("v1"), List.of("v4", "v3", "v2"), null, null);
        Configuration config = configWithDefaultOrder(List.of("v1", "v2", "v3", "v4"));

        List<UnitWebResponse> result = builder.buildOrderedList(allVehicles, rule, config);

        assertThat(result.get(0).id()).isEqualTo("v1"); // alerted
        assertThat(result.get(1).id()).isEqualTo("v4"); // remainingOrder
        assertThat(result.get(2).id()).isEqualTo("v3");
        assertThat(result.get(3).id()).isEqualTo("v2");
    }

    @Test
    void unknownVehicleIdInRuleIsSkipped() {
        Rule rule = new Rule("Brand", List.of("B2"), List.of("v_unknown", "v1"), null, null, null);
        Configuration config = configWithDefaultOrder(List.of("v1", "v2", "v3", "v4"));

        List<UnitWebResponse> result = builder.buildOrderedList(allVehicles, rule, config);

        assertThat(result.get(0).id()).isEqualTo("v1");
        assertThat(result.get(0).alerted()).isTrue();
    }

    @Test
    void emptyVehicleOrderReturnsAllAsNonAlerted() {
        Rule rule = new Rule("Brand", List.of(), List.of(), null, null, null);
        Configuration config = configWithDefaultOrder(List.of("v1", "v2", "v3", "v4"));

        List<UnitWebResponse> result = builder.buildOrderedList(allVehicles, rule, config);

        assertThat(result).hasSize(4);
        assertThat(result).allMatch(v -> !v.alerted());
    }
}