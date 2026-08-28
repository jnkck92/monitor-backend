package de.jkueck.monitor.backend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Unit(

        String id,

        String name,

        String shortName,

        String type,

        String ric,

        Long diveraId

) {
}
