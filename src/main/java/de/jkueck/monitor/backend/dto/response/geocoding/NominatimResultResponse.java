package de.jkueck.monitor.backend.dto.response.geocoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimResultResponse(

        String lat,

        String lon

) {
}