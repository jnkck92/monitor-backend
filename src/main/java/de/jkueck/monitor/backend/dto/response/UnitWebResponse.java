package de.jkueck.monitor.backend.dto.response;

public record UnitWebResponse(

        String id,

        String name,

        String callSign,

        boolean alerted,

        RadioStatusWebResponse radioStatus

) {
}
