package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Profile("dev")
public class MockGeocodingClient implements GeocodingClient {

    @Override
    public Optional<NominatimResultResponse> search(String address) {
        return Optional.of(new NominatimResultResponse("53.1712345", "8.9834567"));
    }

}