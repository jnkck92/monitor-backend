package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.EventWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class EventService {

    private static final int MAX_EVENTS = 4;

    public List<EventWebResponse> buildEventList(EventsResponse eventsResponse) {
        if (eventsResponse == null || eventsResponse.data() == null || eventsResponse.data().items() == null) {
            return List.of();
        }

        Map<String, EventResponse> items = eventsResponse.data().items();

        return items.values().stream()
                .filter(this::isVisible)
                .sorted(Comparator.comparing(EventResponse::date, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_EVENTS)
                .map(this::toWebResponse)
                .toList();
    }

    private boolean isVisible(EventResponse event) {
        return event != null && !Boolean.TRUE.equals(event.deleted());
    }

    private EventWebResponse toWebResponse(EventResponse event) {
        Instant timestamp = event.date() != null ? Instant.ofEpochSecond(event.date()) : null;
        return new EventWebResponse(event.id(), event.title(), event.text(), event.address(),
                timestamp, event.participation(), event.answerable());
    }
}