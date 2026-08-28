package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleStatus(

        Long id,

        Integer fmsstatus

) {}