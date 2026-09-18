package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NewsResponse(

        Long id,

        @JsonProperty("foreign_id") String foreignId,

        String title,

        String text,

        String address,

        Long date,

        Boolean answerable,

        Boolean survey,

        Boolean deleted

) {}