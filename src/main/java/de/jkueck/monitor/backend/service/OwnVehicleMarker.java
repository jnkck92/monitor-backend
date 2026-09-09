package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import org.springframework.stereotype.Component;

@Component
public class OwnVehicleMarker {

    public MonitorWebResponse mark(MonitorWebResponse state, String ownVehicleId) {
        if (state == null || ownVehicleId == null || ownVehicleId.isBlank()) {
            return state;
        }

        return new MonitorWebResponse(
                state.departmentName(),
                state.mode(),
                state.persons(),
                markVehicles(state.vehicles(), ownVehicleId),
                state.alarm(),
                state.lastUpdate(),
                state.error());
    }

    private java.util.List<UnitWebResponse> markVehicles(java.util.List<UnitWebResponse> vehicles, String ownVehicleId) {
        return vehicles.stream()
                .map(v -> withOwnVehicle(v, ownVehicleId.equals(v.id())))
                .toList();
    }

    private UnitWebResponse withOwnVehicle(UnitWebResponse unit, boolean ownVehicle) {
        if (unit.ownVehicle() == ownVehicle) {
            return unit;
        }
        return new UnitWebResponse(unit.id(), unit.name(), unit.callSign(),
                unit.alerted(), unit.radioStatus(), ownVehicle);
    }
}