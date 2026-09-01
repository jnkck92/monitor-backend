package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Status;
import de.jkueck.monitor.backend.dto.configuration.Unit;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnitStatusEnricherTest {

    private final UnitStatusEnricher enricher = new UnitStatusEnricher();

    private Configuration configWithStatuses(Map<String, Status> statuses) {
        return new Configuration("TestFW", List.of(), List.of(), List.of(), null, statuses, List.of());
    }

    @Test
    void enrichReturnsCorrectStatusWhenLiveStatusExists() {
        Unit unit = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 100L);
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 2));
        Configuration config = configWithStatuses(Map.of("2", new Status("Status 2", "#00ff00")));

        UnitWebResponse result = enricher.enrich(unit, live, config);

        assertThat(result.id()).isEqualTo("v1");
        assertThat(result.name()).isEqualTo("LF20");
        assertThat(result.callSign()).isEqualTo("FL-FW 11");
        assertThat(result.alerted()).isTrue();
        assertThat(result.radioStatus().label()).isEqualTo("Status 2");
        assertThat(result.radioStatus().color()).isEqualTo("#00ff00");
    }

    @Test
    void enrichReturnsUnknownStatusWhenNoLiveStatusFound() {
        Unit unit = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 100L);
        List<VehicleStatus> live = List.of(); // kein Match
        Configuration config = configWithStatuses(Map.of());

        UnitWebResponse result = enricher.enrich(unit, live, config);

        assertThat(result.alerted()).isFalse();
        assertThat(result.radioStatus().label()).isEqualTo("Nicht verbunden");
        assertThat(result.radioStatus().color()).isEqualTo("#cccccc");
    }

    @Test
    void enrichFallsBackToDefaultStatusWhenFmsStatusNotInConfig() {
        Unit unit = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 100L);
        List<VehicleStatus> live = List.of(new VehicleStatus(100L, 9));
        Configuration config = configWithStatuses(Map.of("2", new Status("Status 2", "#00ff00"))); // 9 nicht drin

        UnitWebResponse result = enricher.enrich(unit, live, config);

        assertThat(result.alerted()).isTrue();
        assertThat(result.radioStatus().label()).isEqualTo("Unbekannter Status");
        assertThat(result.radioStatus().color()).isEqualTo("#cccccc");
    }

    @Test
    void enrichMatchesByDiveraId() {
        Unit unit = new Unit("v1", "LF20", "LF20", "vehicle", "FL-FW 11", 200L);
        List<VehicleStatus> live = List.of(
                new VehicleStatus(100L, 2),
                new VehicleStatus(200L, 6)
        );
        Configuration config = configWithStatuses(Map.of(
                "2", new Status("Status 2", "#00ff00"),
                "6", new Status("Status 6", "#ff0000")
        ));

        UnitWebResponse result = enricher.enrich(unit, live, config);

        assertThat(result.radioStatus().label()).isEqualTo("Status 6");
    }
}