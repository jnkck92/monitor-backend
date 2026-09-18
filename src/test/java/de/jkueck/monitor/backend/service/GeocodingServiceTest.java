package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.RealGeocodingClient;
import de.jkueck.monitor.backend.dto.response.Coordinates;
import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeocodingServiceTest {

    private RealGeocodingClient realGeocodingClient;
    private GeocodingService geocodingService;

    @BeforeEach
    void setUp() {
        realGeocodingClient = mock(RealGeocodingClient.class);
        geocodingService = new GeocodingService(realGeocodingClient);
    }

    @Test
    @DisplayName("geocode() gibt Koordinaten bei erfolgreichem Treffer zurück")
    void geocodeReturnsCoordinatesOnMatch() {
        when(realGeocodingClient.search("Musterstr. 1"))
                .thenReturn(Optional.of(new NominatimResultResponse("53.1712345", "8.9834567")));

        Optional<Coordinates> result = geocodingService.geocode("Musterstr. 1");

        assertThat(result).isPresent();
        assertThat(result.get().lat()).isEqualTo(53.1712345);
        assertThat(result.get().lon()).isEqualTo(8.9834567);
    }

    @Test
    @DisplayName("geocode() gibt Optional.empty() zurück, wenn der Client nichts findet")
    void geocodeReturnsEmptyWhenClientFindsNothing() {
        when(realGeocodingClient.search(any())).thenReturn(Optional.empty());

        Optional<Coordinates> result = geocodingService.geocode("Unbekannte Adresse");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("geocode() gibt Optional.empty() bei null-Adresse zurück, ohne den Client aufzurufen")
    void geocodeReturnsEmptyForNullAddress() {
        Optional<Coordinates> result = geocodingService.geocode(null);

        assertThat(result).isEmpty();
        verifyNoInteractions(realGeocodingClient);
    }

    @Test
    @DisplayName("geocode() gibt Optional.empty() bei leerer Adresse zurück, ohne den Client aufzurufen")
    void geocodeReturnsEmptyForBlankAddress() {
        Optional<Coordinates> result = geocodingService.geocode("   ");

        assertThat(result).isEmpty();
        verifyNoInteractions(realGeocodingClient);
    }

    @Test
    @DisplayName("geocode() ruft den Client nur einmal pro Adresse auf (Cache-Hit)")
    void geocodeCachesResultsPerAddress() {
        when(realGeocodingClient.search("Musterstr. 1"))
                .thenReturn(Optional.of(new NominatimResultResponse("53.0", "8.0")));

        geocodingService.geocode("Musterstr. 1");
        geocodingService.geocode("Musterstr. 1");
        geocodingService.geocode("Musterstr. 1");

        verify(realGeocodingClient, times(1)).search("Musterstr. 1");
    }

    @Test
    @DisplayName("geocode() bereinigt doppelte, aufeinanderfolgende Wörter vor dem Cache-Lookup")
    void geocodeCleansDuplicatedWords() {
        when(realGeocodingClient.search("Kiepelbergstraße, 27721 Ritterhude"))
                .thenReturn(Optional.of(new NominatimResultResponse("53.17", "8.98")));

        Optional<Coordinates> result = geocodingService.geocode("Kiepelbergstraße, 27721 Ritterhude Ritterhude");

        assertThat(result).isPresent();
        verify(realGeocodingClient).search("Kiepelbergstraße, 27721 Ritterhude");
    }

    @Test
    @DisplayName("geocode() cacht auch negative Ergebnisse (kein erneuter Client-Aufruf)")
    void geocodeCachesEmptyResults() {
        when(realGeocodingClient.search("Unbekannte Adresse")).thenReturn(Optional.empty());

        geocodingService.geocode("Unbekannte Adresse");
        Optional<Coordinates> result = geocodingService.geocode("Unbekannte Adresse");

        assertThat(result).isEmpty();
        verify(realGeocodingClient, times(1)).search("Unbekannte Adresse");
    }
}