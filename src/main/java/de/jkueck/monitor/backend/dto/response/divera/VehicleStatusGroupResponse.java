package de.jkueck.monitor.backend.dto.response.divera;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VehicleStatusGroupResponse(

        boolean success,

        List<VehicleStatus> data

) {}