package de.jkueck.monitor.backend.dto.response;

import java.time.Instant;

public record NewsWebResponse(

        Long id,

        String title,

        String description,

        String address,

        Instant timestamp,

        Boolean survey,

        Boolean answerable

) {
}