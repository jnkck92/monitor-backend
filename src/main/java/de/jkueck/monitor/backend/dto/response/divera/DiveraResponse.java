package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DiveraResponse(

        boolean success,

        Data data

) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(

            Map<String, AlarmResponse> items

    ) {}
    
}