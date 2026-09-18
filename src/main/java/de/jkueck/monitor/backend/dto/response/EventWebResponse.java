package de.jkueck.monitor.backend.dto.response;

import java.time.Instant;

public record EventWebResponse(

        Long id,

        String title,

        String description,

        String address,

        Instant timestamp,

        Integer participation,

        Boolean answerable

) {
}