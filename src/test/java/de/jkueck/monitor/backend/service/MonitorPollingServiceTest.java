package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MonitorPollingServiceTest {

    private DiveraClient client;
    private ConfigurationService configService;
    private MonitorStateBuilder stateBuilder;
    private DiveraResponseLogger responseLogger;

    private MonitorPollingService pollingService;

    private Configuration config;
    private DiveraConfig diveraConfig;
    private DiveraResponse diveraResponse;
    private VehicleStatusGroupResponse vehicleStatusResponse;

    @BeforeEach
    void setUp() {
        client = mock(DiveraClient.class);
        configService = mock(ConfigurationService.class);
        stateBuilder = mock(MonitorStateBuilder.class);
        responseLogger = mock(DiveraResponseLogger.class);

        MeterRegistry meterRegistry = new SimpleMeterRegistry();

        pollingService = new MonitorPollingService(client, configService, stateBuilder, responseLogger, meterRegistry);

        diveraConfig = new DiveraConfig("test-key", "https://www.divera247.com");
        config = new Configuration("TestFW", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        diveraResponse = new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
        vehicleStatusResponse = new VehicleStatusGroupResponse(true, List.of());
    }

    @Test
    @DisplayName("getCurrentState() wirft Exception wenn Tenant noch nicht gepollt wurde")
    void getCurrentStateThrowsWhenTenantUnknown() {
        assertThatThrownBy(() -> pollingService.getCurrentState("unbekannt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unbekannt");
    }

    @Test
    @DisplayName("poll() aktualisiert den currentState für den jeweiligen Tenant")
    void pollUpdatesCurrentStateForTenant() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(diveraResponse, List.of(), config)).thenReturn(newState);

        pollingService.poll();

        assertThat(pollingService.getCurrentState("musterstadt")).isEqualTo(newState);
    }

    @Test
    @DisplayName("poll() verarbeitet mehrere Tenants unabhängig voneinander")
    void pollHandlesMultipleTenantsIndependently() {
        when(configService.getKnownTenants()).thenReturn(List.of("tenant-a", "tenant-b"));

        Configuration configA = new Configuration("FW A", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        Configuration configB = new Configuration("FW B", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());

        MonitorWebResponse stateA = new MonitorWebResponse("FW A", "STANDBY", List.of(), List.of(), null, Instant.now(), null);
        MonitorWebResponse stateB = new MonitorWebResponse("FW B", "ALARM", List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("tenant-a")).thenReturn(configA);
        when(configService.getConfigForTenant("tenant-b")).thenReturn(configB);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(diveraResponse, List.of(), configA)).thenReturn(stateA);
        when(stateBuilder.build(diveraResponse, List.of(), configB)).thenReturn(stateB);

        pollingService.poll();

        assertThat(pollingService.getCurrentState("tenant-a").mode()).isEqualTo("STANDBY");
        assertThat(pollingService.getCurrentState("tenant-b").mode()).isEqualTo("ALARM");
    }

    @Test
    @DisplayName("poll() ruft responseLogger.logIfChanged auf")
    void pollCallsResponseLogger() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(newState);

        pollingService.poll();

        verify(responseLogger).logIfChanged(diveraResponse);
    }

    @Test
    @DisplayName("poll() übergibt VehicleStatus-Daten an stateBuilder")
    void pollPassesVehicleStatusToBuilder() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        List<VehicleStatus> liveStatuses = List.of(new VehicleStatus(100L, 2));
        vehicleStatusResponse = new VehicleStatusGroupResponse(true, liveStatuses);
        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(diveraResponse, liveStatuses, config)).thenReturn(newState);

        pollingService.poll();

        verify(stateBuilder).build(diveraResponse, liveStatuses, config);
    }

    @Test
    @DisplayName("poll() setzt error im State wenn Config nicht geladen werden kann")
    void pollSetsErrorOnConfigException() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt"))
                .thenThrow(new RuntimeException("Config nicht erreichbar"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState("musterstadt");
        assertThat(state.error()).isEqualTo("Config nicht erreichbar");
    }

    @Test
    @DisplayName("poll() behält bisherigen State bei Exception")
    void pollKeepsPreviousStateOnException() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        MonitorWebResponse firstState = new MonitorWebResponse("TestFW", "ALARM",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(firstState);

        pollingService.poll();

        reset(configService);
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt")).thenThrow(new RuntimeException("Fehler"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState("musterstadt");
        assertThat(state.mode()).isEqualTo("ALARM");
        assertThat(state.departmentName()).isEqualTo("TestFW");
        assertThat(state.error()).isEqualTo("Fehler");
    }

    @Test
    @DisplayName("poll() setzt Fehler wenn kein Divera accessKey konfiguriert ist")
    void pollSetsErrorWhenNoDiveraAccessKeyConfigured() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        Configuration configWithoutKey = new Configuration("TestFW",
                new DiveraConfig(null, null), List.of(), List.of(), List.of(), null, Map.of(), List.of());

        when(configService.getConfigForTenant("musterstadt")).thenReturn(configWithoutKey);

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState("musterstadt");
        assertThat(state.error()).contains("accessKey");
    }

    @Test
    @DisplayName("poll() bei client.pullAll() Exception setzt Fehler")
    void pollHandlesPullAllException() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenThrow(new RuntimeException("API timeout"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState("musterstadt");
        assertThat(state.error()).isEqualTo("API timeout");
    }

    @Test
    @DisplayName("poll() bei client.pullVehicleStatus() Exception setzt Fehler")
    void pollHandlesPullVehicleStatusException() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenThrow(new RuntimeException("Vehicle API down"));

        pollingService.poll();

        MonitorWebResponse state = pollingService.getCurrentState("musterstadt");
        assertThat(state.error()).isEqualTo("Vehicle API down");
    }

    @Test
    @DisplayName("poll() macht nichts, wenn keine Tenants bekannt sind")
    void pollDoesNothingWhenNoTenantsKnown() {
        when(configService.getKnownTenants()).thenReturn(List.of());

        pollingService.poll();

        verifyNoInteractions(client, stateBuilder, responseLogger);
        assertThat(pollingService.getAllStates()).isEmpty();
    }

    @Test
    @DisplayName("initialPoll() ruft poll() für alle bekannten Tenants auf")
    void initialPollCallsPoll() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        MonitorWebResponse newState = new MonitorWebResponse("TestFW", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(newState);

        pollingService.initialPoll();

        verify(client).pullAll(diveraConfig);
        verify(client).pullVehicleStatus(diveraConfig);
        verify(stateBuilder).build(any(), any(), any());
    }

    @Test
    @DisplayName("mehrere erfolgreiche Polls überschreiben State korrekt")
    void multipleSuccessfulPollsUpdateState() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));

        MonitorWebResponse state1 = new MonitorWebResponse("FW1", "STANDBY",
                List.of(), List.of(), null, Instant.now(), null);
        MonitorWebResponse state2 = new MonitorWebResponse("FW1", "ALARM",
                List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(any(), any(), any())).thenReturn(state1, state2);

        pollingService.poll();
        assertThat(pollingService.getCurrentState("musterstadt").mode()).isEqualTo("STANDBY");

        pollingService.poll();
        assertThat(pollingService.getCurrentState("musterstadt").mode()).isEqualTo("ALARM");
    }

    @Test
    @DisplayName("getAllStates() liefert States aller gepollten Tenants")
    void getAllStatesReturnsAllTenantStates() {
        when(configService.getKnownTenants()).thenReturn(List.of("tenant-a", "tenant-b"));

        Configuration configA = new Configuration("FW A", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        Configuration configB = new Configuration("FW B", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());

        MonitorWebResponse stateA = new MonitorWebResponse("FW A", "STANDBY", List.of(), List.of(), null, Instant.now(), null);
        MonitorWebResponse stateB = new MonitorWebResponse("FW B", "ALARM", List.of(), List.of(), null, Instant.now(), null);

        when(configService.getConfigForTenant("tenant-a")).thenReturn(configA);
        when(configService.getConfigForTenant("tenant-b")).thenReturn(configB);
        when(client.pullAll(diveraConfig)).thenReturn(diveraResponse);
        when(client.pullVehicleStatus(diveraConfig)).thenReturn(vehicleStatusResponse);
        when(stateBuilder.build(diveraResponse, List.of(), configA)).thenReturn(stateA);
        when(stateBuilder.build(diveraResponse, List.of(), configB)).thenReturn(stateB);

        pollingService.poll();

        Map<String, MonitorWebResponse> allStates = pollingService.getAllStates();
        assertThat(allStates).containsKeys("tenant-a", "tenant-b");
        assertThat(allStates.get("tenant-a").mode()).isEqualTo("STANDBY");
        assertThat(allStates.get("tenant-b").mode()).isEqualTo("ALARM");
    }

    @Test
    @DisplayName("getAllStates() gibt leere Map zurück wenn noch nie gepollt wurde")
    void getAllStatesReturnsEmptyMapInitially() {
        assertThat(pollingService.getAllStates()).isEmpty();
    }
}