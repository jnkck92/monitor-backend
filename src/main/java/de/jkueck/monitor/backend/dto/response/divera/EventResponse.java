package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventResponse(

        Long id,

        @JsonProperty("foreign_id") String foreignId,

        String title,

        String text,

        String address,

        Long date,

        Boolean answerable,

        Integer participation,

        String note,

        Boolean deleted

) {}