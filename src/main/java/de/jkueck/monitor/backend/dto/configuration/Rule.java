package de.jkueck.monitor.backend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Rule(

        String label,

        List<String> keywords,

        List<String> vehicleOrder,

        List<String> remainingOrder

) {
}
