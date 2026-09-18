package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;

import java.util.Optional;

public interface GeocodingClient {

    Optional<NominatimResultResponse> search(String address);

}
