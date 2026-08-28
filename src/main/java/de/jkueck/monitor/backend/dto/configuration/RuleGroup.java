package de.jkueck.monitor.backend.dto.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RuleGroup(

        String category,

        String label,

        String color,

        List<Rule> rules

) {
}
