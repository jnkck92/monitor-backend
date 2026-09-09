package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.AlarmWebResponse;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OwnVehicleMarkerTest {

    private final OwnVehicleMarker marker = new OwnVehicleMarker();

    private static final RadioStatusWebResponse STATUS = new RadioStatusWebResponse("BEREIT", "#00ff00");

    private UnitWebResponse vehicle(String id, boolean ownVehicle) {
        return new UnitWebResponse(id, "Name-" + id, "RIC-" + id, false, STATUS, ownVehicle);
    }

    private MonitorWebResponse stateWithVehicles(List<UnitWebResponse> vehicles, List<UnitWebResponse> persons) {
        return new MonitorWebResponse("TestFW", "STANDBY", persons, vehicles, null, Instant.now(), null);
    }

    @Test
    @DisplayName("gibt null zurück wenn der State null ist")
    void returnsNullWhenStateIsNull() {
        assertThat(marker.mark(null, "elw1")).isNull();
    }

    @Test
    @DisplayName("gibt State unverändert zurück wenn ownVehicleId null ist")
    void returnsStateUnchangedWhenOwnVehicleIdIsNull() {
        MonitorWebResponse state = stateWithVehicles(List.of(vehicle("elw1", false)), List.of());

        MonitorWebResponse result = marker.mark(state, null);

        assertThat(result).isSameAs(state);
    }

    @Test
    @DisplayName("gibt State unverändert zurück wenn ownVehicleId leer/blank ist")
    void returnsStateUnchangedWhenOwnVehicleIdIsBlank() {
        MonitorWebResponse state = stateWithVehicles(List.of(vehicle("elw1", false)), List.of());

        MonitorWebResponse result = marker.mark(state, "   ");

        assertThat(result).isSameAs(state);
    }

    @Test
    @DisplayName("markiert das passende Fahrzeug als ownVehicle")
    void marksMatchingVehicleAsOwnVehicle() {
        MonitorWebResponse state = stateWithVehicles(
                List.of(vehicle("elw1", false), vehicle("hlf20", false)), List.of());

        MonitorWebResponse result = marker.mark(state, "elw1");

        assertThat(result.vehicles()).hasSize(2);
        assertThat(result.vehicles().get(0).id()).isEqualTo("elw1");
        assertThat(result.vehicles().get(0).ownVehicle()).isTrue();
        assertThat(result.vehicles().get(1).id()).isEqualTo("hlf20");
        assertThat(result.vehicles().get(1).ownVehicle()).isFalse();
    }

    @Test
    @DisplayName("setzt ownVehicle=false wenn kein Fahrzeug passt")
    void marksNoVehicleWhenNoneMatches() {
        MonitorWebResponse state = stateWithVehicles(
                List.of(vehicle("elw1", false), vehicle("hlf20", false)), List.of());

        MonitorWebResponse result = marker.mark(state, "unbekannt");

        assertThat(result.vehicles()).allMatch(v -> !v.ownVehicle());
    }

    @Test
    @DisplayName("entfernt vorheriges ownVehicle-Flag wenn ein anderes Fahrzeug jetzt passt")
    void unmarksPreviouslyMarkedVehicleWhenNoLongerMatching() {
        MonitorWebResponse state = stateWithVehicles(
                List.of(vehicle("elw1", true), vehicle("hlf20", false)), List.of());

        MonitorWebResponse result = marker.mark(state, "hlf20");

        assertThat(result.vehicles().get(0).id()).isEqualTo("elw1");
        assertThat(result.vehicles().get(0).ownVehicle()).isFalse();
        assertThat(result.vehicles().get(1).id()).isEqualTo("hlf20");
        assertThat(result.vehicles().get(1).ownVehicle()).isTrue();
    }

    @Test
    @DisplayName("lässt persons unverändert, auch wenn eine id zufällig übereinstimmt")
    void doesNotTouchPersons() {
        UnitWebResponse person = new UnitWebResponse("elw1", "Max", "P1", false, STATUS, false);
        MonitorWebResponse state = stateWithVehicles(List.of(vehicle("elw1", false)), List.of(person));

        MonitorWebResponse result = marker.mark(state, "elw1");

        assertThat(result.persons()).containsExactly(person);
    }

    @Test
    @DisplayName("behält alle anderen Felder des MonitorWebResponse unverändert bei")
    void keepsOtherFieldsUnchanged() {
        AlarmWebResponse alarm = new AlarmWebResponse("F 01 - Kleinbrand", "F 01", "Kleinbrand",
                "Musterstr. 1", "Kleinbrand", "#b30000", "Hinweis", Instant.parse("2026-09-01T12:00:00Z"));
        Instant timestamp = Instant.parse("2026-09-01T12:00:00Z");
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "ALARM",
                List.of(), List.of(vehicle("elw1", false)), alarm, timestamp, "err");

        MonitorWebResponse result = marker.mark(state, "elw1");

        assertThat(result.departmentName()).isEqualTo("TestFW");
        assertThat(result.mode()).isEqualTo("ALARM");
        assertThat(result.alarm()).isEqualTo(alarm);
        assertThat(result.lastUpdate()).isEqualTo(timestamp);
        assertThat(result.error()).isEqualTo("err");
    }

    @Test
    @DisplayName("gibt dasselbe Vehicle-Objekt zurück wenn sich das Flag nicht ändert")
    void returnsSameInstanceWhenFlagUnchanged() {
        UnitWebResponse unchangedVehicle = vehicle("hlf20", false);
        MonitorWebResponse state = stateWithVehicles(
                List.of(vehicle("elw1", false), unchangedVehicle), List.of());

        MonitorWebResponse result = marker.mark(state, "elw1");

        assertThat(result.vehicles().get(1)).isSameAs(unchangedVehicle);
    }
}