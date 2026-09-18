package de.jkueck.monitor.backend.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.jkueck.monitor.backend.config.GeocodingProperties;
import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@WireMockTest
class RealGeocodingClientTest {

    private RealGeocodingClient createClient(String baseUrl) {
        return createClient(baseUrl, Duration.ofSeconds(5), Duration.ofSeconds(5));
    }

    private RealGeocodingClient createClient(String baseUrl, Duration connectTimeout, Duration readTimeout) {
        GeocodingProperties properties = new GeocodingProperties(baseUrl, "monitor-backend-test/1.0", connectTimeout, readTimeout);
        return new RealGeocodingClient(RestClient.builder(), properties, new SimpleMeterRegistry());
    }

    @Test
    @DisplayName("search() gibt erstes Ergebnis bei erfolgreicher Antwort zurück")
    void searchReturnsFirstResult(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/search"))
                .withQueryParam("q", equalTo("Kiepelbergstraße, 27721 Ritterhude"))
                .willReturn(okJson("""
                        [
                          {"lat": "53.1712345", "lon": "8.9834567"}
                        ]
                        """)));

        RealGeocodingClient client = createClient(wm.getHttpBaseUrl());
        Optional<NominatimResultResponse> result = client.search("Kiepelbergstraße, 27721 Ritterhude");

        assertThat(result).isPresent();
        assertThat(result.get().lat()).isEqualTo("53.1712345");
        assertThat(result.get().lon()).isEqualTo("8.9834567");
    }

    @Test
    @DisplayName("search() sendet User-Agent Header")
    void searchSendsUserAgentHeader(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(okJson("[]")));

        RealGeocodingClient client = createClient(wm.getHttpBaseUrl());
        client.search("Musterstr. 1");

        verify(getRequestedFor(urlPathEqualTo("/search"))
                .withHeader("User-Agent", equalTo("monitor-backend-test/1.0")));
    }

    @Test
    @DisplayName("search() gibt Optional.empty() bei leerer Ergebnisliste zurück")
    void searchReturnsEmptyWhenNoResults(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(okJson("[]")));

        RealGeocodingClient client = createClient(wm.getHttpBaseUrl());
        Optional<NominatimResultResponse> result = client.search("Unbekannte Adresse");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("search() gibt Optional.empty() bei Server-Fehler zurück")
    void searchReturnsEmptyOnServerError(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(serverError()));

        RealGeocodingClient client = createClient(wm.getHttpBaseUrl());
        Optional<NominatimResultResponse> result = client.search("Musterstr. 1");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("search() gibt Optional.empty() bei Timeout zurück")
    void searchReturnsEmptyOnTimeout(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/search"))
                .willReturn(ok().withFixedDelay(2000)));

        RealGeocodingClient client = createClient(wm.getHttpBaseUrl(), Duration.ofSeconds(5), Duration.ofMillis(500));
        Optional<NominatimResultResponse> result = client.search("Musterstr. 1");

        assertThat(result).isEmpty();
    }
}