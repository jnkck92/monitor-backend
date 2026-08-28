package de.jkueck.monitor.backend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Status(

        String label,

        String color

) {
}