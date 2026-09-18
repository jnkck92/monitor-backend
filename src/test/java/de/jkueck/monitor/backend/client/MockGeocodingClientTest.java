package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MockGeocodingClientTest {

    private final MockGeocodingClient client = new MockGeocodingClient();

    @Test
    @DisplayName("search() gibt feste Test-Koordinaten zurück, ohne echten HTTP-Call")
    void searchReturnsFixedCoordinates() {
        Optional<NominatimResultResponse> result = client.search("Kiepelbergstraße, 27721 Ritterhude Ritterhude");

        assertThat(result).isPresent();
        assertThat(result.get().lat()).isEqualTo("53.1712345");
        assertThat(result.get().lon()).isEqualTo("8.9834567");
    }

    @Test
    @DisplayName("search() liefert Koordinaten unabhängig von der übergebenen Adresse")
    void searchIgnoresAddressContent() {
        assertThat(client.search("irgendeine Adresse")).isPresent();
        assertThat(client.search(null)).isPresent();
    }
}