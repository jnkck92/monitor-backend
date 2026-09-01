package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MonitorPollingServiceTest {

    @Mock
    private DiveraClient client;

    @Mock
    private ConfigurationService configService;

    @Mock
    private MonitorStateBuilder stateBuilder;

    @Mock
    private DiveraResponseLogger responseLogger;

    @InjectMocks
    private MonitorPollingService pollingService;

    private Configuration config;
    private DiveraResponse diveraResponse;
    private VehicleStatusGroupResponse vehicleStatusResponse;

    @BeforeEach
    void setUp() {
        config = new Configuration("TestFW", List.of(), List.of(), List.of(), null, Map.of(), List.of());
        diveraResponse = new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
        vehicleStatusResponse = new VehicleStatusGroupResponse(true, List.of());
    }

    @Test
    @DisplayName("poll() aktualisiert den currentState")
    void pollUpdatesCurrentState() {
        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(eq(diveraResponse), eq(List.of()), eq(config))).thenReturn(newState);

        pollingService.poll();

        assertThat(pollingService.getCurrentState().get()).isEqualTo(newState);
    }

    @Test
    @DisplayName("poll() ruft responseLogger.logIfChanged auf")
    void pollCallsResponseLogger() {
        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(newState);

        pollingService.poll();

        verify(responseLogger).logIfChanged(diveraResponse);
    }

    @Test
    @DisplayName("poll() übergibt VehicleStatus-Daten an stateBuilder")
    void pollPassesVehicleStatusToBuilder() {
        List<VehicleStatus> liveStatuses = List.of(new VehicleStatus(100L, 2));
        vehicleStatusResponse = new VehicleStatusGroupResponse(true, liveStatuses);
        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(eq(diveraResponse), eq(liveStatuses), eq(config))).thenReturn(newState);

        pollingService.poll();

        verify(stateBuilder).build(diveraResponse, liveStatuses, config);
    }

    @Test
    @DisplayName("poll() setzt error im State wenn Exception auftritt")
    void pollSetsErrorOnException() {
        when(configService.getConfig()).thenThrow(new RuntimeException("Config nicht erreichbar"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState().get();
        assertThat(state.error()).isEqualTo("Config nicht erreichbar");
    }

    @Test
    @DisplayName("poll() behält bisherigen State bei Exception")
    void pollKeepsPreviousStateOnException() {
        // Erst erfolgreicher Poll
        MonitorWebResponse firstState = new MonitorWebResponse("TestFW", "ALARM",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(firstState);

        pollingService.poll();

        // Dann fehlerhafter Poll
        reset(configService);
        when(configService.getConfig()).thenThrow(new RuntimeException("Fehler"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState().get();
        assertThat(state.mode()).isEqualTo("ALARM");
        assertThat(state.departmentName()).isEqualTo("TestFW");
        assertThat(state.error()).isEqualTo("Fehler");
    }

    @Test
    @DisplayName("poll() bei client.pullAll() Exception setzt Fehler")
    void pollHandlesPullAllException() {
        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenThrow(new RuntimeException("API timeout"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState().get();
        assertThat(state.error()).isEqualTo("API timeout");
    }

    @Test
    @DisplayName("poll() bei client.pullVehicleStatus() Exception setzt Fehler")
    void pollHandlesPullVehicleStatusException() {
        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenThrow(new RuntimeException("Vehicle API down"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState().get();
        assertThat(state.error()).isEqualTo("Vehicle API down");
    }

    @Test
    @DisplayName("initialPoll() ruft poll() auf")
    void initialPollCallsPoll() {
        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(newState);

        pollingService.initialPoll();

        verify(client).pullAll();
        verify(client).pullVehicleStatus();
        verify(stateBuilder).build(any(), any(), any());
    }

    @Test
    @DisplayName("currentState hat sinnvollen Initialwert")
    void currentStateHasDefaultValue() {
        MonitorWebResponse initial = pollingService.getCurrentState().get();

        assertThat(initial.departmentName()).isEqualTo("DEFAULT");
        assertThat(initial.mode()).isEqualTo("STANDBY");
        assertThat(initial.persons()).isEmpty();
        assertThat(initial.vehicles()).isEmpty();
        assertThat(initial.alarm()).isNull();
        assertThat(initial.error()).isNull();
    }

    @Test
    @DisplayName("mehrere erfolgreiche Polls überschreiben State korrekt")
    void multipleSuccessfulPollsUpdateState() {
        MonitorWebResponse state1 = new MonitorWebResponse("FW1", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);
        MonitorWebResponse state2 = new MonitorWebResponse("FW1", "ALARM",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfig()).thenReturn(config);
        when(client.pullAll()).thenReturn(diveraResponse);
        when(client.pullVehicleStatus()).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(state1, state2);

        pollingService.poll();
        assertThat(pollingService.getCurrentState().get().mode()).isEqualTo("STANDBY");

        pollingService.poll();
        assertThat(pollingService.getCurrentState().get().mode()).isEqualTo("ALARM");
    }
}