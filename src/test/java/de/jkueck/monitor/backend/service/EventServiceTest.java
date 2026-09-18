package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.EventWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventServiceTest {

    private final EventService service = new EventService();

    @Test
    void buildEventListReturnsEmptyListWhenResponseIsNull() {
        assertThat(service.buildEventList(null)).isEmpty();
    }

    @Test
    void buildEventListReturnsEmptyListWhenDataIsNull() {
        EventsResponse response = new EventsResponse(true, null);

        assertThat(service.buildEventList(response)).isEmpty();
    }

    @Test
    void buildEventListMapsFieldsCorrectly() {
        long epochSeconds = 1725192000L;
        EventResponse event = new EventResponse(1L, "ext-1", "Übung", "Beschreibung", "Musterstr. 1",
                epochSeconds, true, 1, "bin dabei", false);
        EventsResponse response = new EventsResponse(true, new EventsResponse.Data(Map.of("1", event), List.of(1L)));

        List<EventWebResponse> result = service.buildEventList(response);

        assertThat(result).hasSize(1);
        EventWebResponse webEvent = result.get(0);
        assertThat(webEvent.id()).isEqualTo(1L);
        assertThat(webEvent.title()).isEqualTo("Übung");
        assertThat(webEvent.description()).isEqualTo("Beschreibung");
        assertThat(webEvent.address()).isEqualTo("Musterstr. 1");
        assertThat(webEvent.timestamp()).isEqualTo(Instant.ofEpochSecond(epochSeconds));
        assertThat(webEvent.participation()).isEqualTo(1);
        assertThat(webEvent.answerable()).isTrue();
    }

    @Test
    void buildEventListFiltersDeletedEvents() {
        EventResponse deleted = new EventResponse(1L, null, "Gelöscht", null, null, null, false, null, null, true);
        EventsResponse response = new EventsResponse(true, new EventsResponse.Data(Map.of("1", deleted), null));

        assertThat(service.buildEventList(response)).isEmpty();
    }

    @Test
    void buildEventListRespectsSortingOrder() {
        EventResponse first = new EventResponse(1L, null, "Erster", null, null, null, false, null, null, false);
        EventResponse second = new EventResponse(2L, null, "Zweiter", null, null, null, false, null, null, false);
        EventsResponse response = new EventsResponse(true,
                new EventsResponse.Data(Map.of("1", first, "2", second), List.of(2L, 1L)));

        List<EventWebResponse> result = service.buildEventList(response);

        assertThat(result).extracting(EventWebResponse::title).containsExactly("Zweiter", "Erster");
    }

    @Test
    void buildEventListFallsBackToUnsortedWhenSortingIsMissing() {
        EventResponse event = new EventResponse(1L, null, "Ohne Sortierung", null, null, null, false, null, null, false);
        EventsResponse response = new EventsResponse(true, new EventsResponse.Data(Map.of("1", event), List.of()));

        assertThat(service.buildEventList(response)).hasSize(1);
    }

    @Test
    void buildEventListReturnsNewestFourEventsSortedByDateDescending() {
        EventResponse oldest = new EventResponse(1L, null, "Ältestes", null, null, 1000L, false, null, null, false);
        EventResponse newest = new EventResponse(2L, null, "Neuestes", null, null, 5000L, false, null, null, false);
        EventResponse middle = new EventResponse(3L, null, "Mittleres", null, null, 3000L, false, null, null, false);
        EventResponse extra1 = new EventResponse(4L, null, "Extra1", null, null, 2000L, false, null, null, false);
        EventResponse extra2 = new EventResponse(5L, null, "Extra2", null, null, 500L, false, null, null, false);
        EventsResponse response = new EventsResponse(true,
                new EventsResponse.Data(Map.of("1", oldest, "2", newest, "3", middle, "4", extra1, "5", extra2), null));

        List<EventWebResponse> result = service.buildEventList(response);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(EventWebResponse::title)
                .containsExactly("Neuestes", "Mittleres", "Extra1", "Ältestes");
    }

    @Test
    void buildEventListSortsEventsWithoutDateLast() {
        EventResponse withDate = new EventResponse(1L, null, "Mit Datum", null, null, 1000L, false, null, null, false);
        EventResponse withoutDate = new EventResponse(2L, null, "Ohne Datum", null, null, null, false, null, null, false);
        EventsResponse response = new EventsResponse(true,
                new EventsResponse.Data(Map.of("1", withDate, "2", withoutDate), null));

        List<EventWebResponse> result = service.buildEventList(response);

        assertThat(result).extracting(EventWebResponse::title).containsExactly("Mit Datum", "Ohne Datum");
    }

}