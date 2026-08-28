package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlarmResponse(

        Long id,

        String title,

        String text,

        String address,

        Long date,

        Boolean closed,

        Boolean priority

) {}