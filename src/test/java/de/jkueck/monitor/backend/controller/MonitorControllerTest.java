package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.dto.response.AlarmWebResponse;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.RadioStatusWebResponse;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitorController.class)
class MonitorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MonitorPollingService pollingService;

    @Test
    @DisplayName("GET /api/v1/monitor/status gibt STANDBY-State zurück")
    void getStatusReturnsStandbyState() throws Exception {
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.parse("2026-09-01T12:00:00Z"), null);

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.departmentName").value("TestFW"))
                .andExpect(jsonPath("$.mode").value("STANDBY"))
                .andExpect(jsonPath("$.alarm").isEmpty())
                .andExpect(jsonPath("$.error").isEmpty())
                .andExpect(jsonPath("$.persons").isArray())
                .andExpect(jsonPath("$.persons").isEmpty())
                .andExpect(jsonPath("$.vehicles").isArray())
                .andExpect(jsonPath("$.vehicles").isEmpty());
    }

    @Test
    @DisplayName("GET /api/v1/monitor/status gibt ALARM-State mit Alarm-Details zurück")
    void getStatusReturnsAlarmState() throws Exception {
        AlarmWebResponse alarm = new AlarmWebResponse("B2 Zimmerbrand", "Musterstr. 1", "Zimmerbrand", "#ff0000", "Atemschutz bereitstellen");
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "ALARM",
                List.of(), List.of(), alarm, Instant.parse("2026-09-01T12:00:00Z"), null);

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("ALARM"))
                .andExpect(jsonPath("$.alarm.title").value("B2 Zimmerbrand"))
                .andExpect(jsonPath("$.alarm.address").value("Musterstr. 1"))
                .andExpect(jsonPath("$.alarm.label").value("Zimmerbrand"))
                .andExpect(jsonPath("$.alarm.color").value("#ff0000"))
                .andExpect(jsonPath("$.alarm.hint").value("Atemschutz bereitstellen"));
    }

    @Test
    @DisplayName("GET /api/v1/monitor/status gibt Vehicles und Persons zurück")
    void getStatusReturnsVehiclesAndPersons() throws Exception {
        UnitWebResponse vehicle = new UnitWebResponse("v1", "LF20", "FL-FW 11", true,
                new RadioStatusWebResponse("Status 2", "#00ff00"));
        UnitWebResponse person = new UnitWebResponse("p1", "Max", "P1", false,
                new RadioStatusWebResponse("UNBEKANNT", "#cccccc"));

        MonitorWebResponse state = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(person), List.of(vehicle), null, Instant.parse("2026-09-01T12:00:00Z"), null);

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicles[0].id").value("v1"))
                .andExpect(jsonPath("$.vehicles[0].name").value("LF20"))
                .andExpect(jsonPath("$.vehicles[0].callSign").value("FL-FW 11"))
                .andExpect(jsonPath("$.vehicles[0].alerted").value(true))
                .andExpect(jsonPath("$.vehicles[0].radioStatus.label").value("Status 2"))
                .andExpect(jsonPath("$.vehicles[0].radioStatus.color").value("#00ff00"))
                .andExpect(jsonPath("$.persons[0].id").value("p1"))
                .andExpect(jsonPath("$.persons[0].alerted").value(false));
    }

    @Test
    @DisplayName("GET /api/v1/monitor/status gibt Error-State zurück")
    void getStatusReturnsErrorState() throws Exception {
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.parse("2026-09-01T12:00:00Z"), "API timeout");

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("API timeout"))
                .andExpect(jsonPath("$.mode").value("STANDBY"));
    }

    @Test
    @DisplayName("GET /api/v1/monitor/status liefert JSON Content-Type")
    void getStatusReturnsJson() throws Exception {
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"));
    }

    @Test
    @DisplayName("GET /api/v1/monitor/status gibt lastUpdate zurück")
    void getStatusReturnsLastUpdate() throws Exception {
        Instant timestamp = Instant.parse("2026-09-01T12:00:00Z");
        MonitorWebResponse state = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, timestamp, null);

        when(pollingService.getCurrentState()).thenReturn(new AtomicReference<>(state));

        mockMvc.perform(get("/api/v1/monitor/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastUpdate").value("2026-09-01T12:00:00Z"));
    }
}