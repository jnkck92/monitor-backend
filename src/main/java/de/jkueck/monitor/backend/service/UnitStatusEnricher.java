package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Status;
import de.jkueck.monitor.backend.dto.configuration.Unit;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UnitStatusEnricher {

    private static final String UNKNOWN_COLOR = "#cccccc";
    private static final Status DEFAULT_STATUS = new Status("Unbekannter Status", UNKNOWN_COLOR);
    private static final RadioStatusWebResponse NO_CONNECTION_STATUS = new RadioStatusWebResponse("Nicht verbunden", UNKNOWN_COLOR);

    public UnitWebResponse enrich(Unit unit, List<VehicleStatus> liveStatuses, Configuration configuration) {
        return liveStatuses.stream()
                .filter(s -> s.id().equals(unit.diveraId()))
                .findFirst()
                .map(live -> withLiveStatus(unit, live, configuration))
                .orElseGet(() -> withoutLiveStatus(unit));
    }

    private UnitWebResponse withLiveStatus(Unit unit, VehicleStatus live, Configuration configuration) {
        Status status = configuration.statuses()
                .getOrDefault(String.valueOf(live.fmsstatus()), DEFAULT_STATUS);
        return new UnitWebResponse(unit.id(), unit.shortName(), unit.ric(), true,
                new RadioStatusWebResponse(status.label(), status.color()));
    }

    private UnitWebResponse withoutLiveStatus(Unit unit) {
        return new UnitWebResponse(unit.id(), unit.shortName(), unit.ric(), false, NO_CONNECTION_STATUS);
    }

}