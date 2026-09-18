package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.EventResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventsResponse;
import de.jkueck.monitor.backend.service.ConfigurationProvider;
import de.jkueck.monitor.backend.service.EventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventController.class)
class EventControllerTest {

    private static final String TENANT = "musterstadt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiveraClient client;

    @MockitoBean
    private ConfigurationProvider configService;

    @MockitoBean
    private EventService eventService;

    @Test
    @DisplayName("GET /api/v1/events gibt Termine für den angegebenen Tenant zurück")
    void getEventsReturnsEvents() throws Exception {
        DiveraConfig diveraConfig = new DiveraConfig("test-key", "https://app.divera247.com/api");
        Configuration config = new Configuration("TestFW", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        EventsResponse eventsResponse = new EventsResponse(true, new EventsResponse.Data(Map.of(), List.of()));

        when(configService.getConfigForTenant(TENANT)).thenReturn(config);
        when(client.pullEvents(diveraConfig)).thenReturn(eventsResponse);
        when(eventService.buildEventList(eventsResponse)).thenReturn(List.of(
                new de.jkueck.monitor.backend.dto.response.EventWebResponse(1L, "Übung", "Beschreibung",
                        "Musterstr. 1", null, 1, true)
        ));

        mockMvc.perform(get("/api/v1/events").header("X-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Übung"))
                .andExpect(jsonPath("$[0].address").value("Musterstr. 1"));
    }

    @Test
    @DisplayName("GET /api/v1/events ohne X-Tenant Header liefert 400")
    void getEventsWithoutTenantHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isBadRequest());
    }
}