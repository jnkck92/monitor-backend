package de.jkueck.monitor.backend.dto.response;

import java.time.Instant;
import java.util.List;

public record MonitorWebResponse(

        String departmentName,

        String mode,

        List<UnitWebResponse> persons,

        List<UnitWebResponse> vehicles,

        AlarmWebResponse alarm,

        Instant lastUpdate,

        String error

) {
}
