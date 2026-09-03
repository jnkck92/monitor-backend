package de.jkueck.monitor.backend.config;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.service.ConfigurationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DiveraConnectionValidatorTest {

    private DiveraClient client;
    private ConfigurationService configService;
    private DiveraConnectionValidator validator;

    private DiveraConfig diveraConfig;
    private Configuration config;

    @BeforeEach
    void setUp() {
        client = mock(DiveraClient.class);
        configService = mock(ConfigurationService.class);
        validator = new DiveraConnectionValidator(client, configService);

        diveraConfig = new DiveraConfig("test-key", "https://www.divera247.com");
        config = new Configuration("TestFW", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
    }

    @Test
    @DisplayName("validateConnection() validiert alle bekannten Tenants")
    void validatesAllKnownTenants() {
        when(configService.getKnownTenants()).thenReturn(List.of("tenant-a", "tenant-b"));

        Configuration configA = new Configuration("FW A", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        Configuration configB = new Configuration("FW B", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());

        when(configService.getConfigForTenant("tenant-a")).thenReturn(configA);
        when(configService.getConfigForTenant("tenant-b")).thenReturn(configB);
        when(client.pullAll(diveraConfig)).thenReturn(new DiveraResponse(true, new DiveraResponse.Data(Map.of())));

        validator.validateConnection();

        verify(client, times(2)).pullAll(diveraConfig);
    }

    @Test
    @DisplayName("validateConnection() wirft Exception wenn Divera success=false zurückgibt")
    void throwsWhenDiveraReturnsFailure() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenReturn(new DiveraResponse(false, new DiveraResponse.Data(Map.of())));

        assertThatThrownBy(() -> validator.validateConnection())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("musterstadt");
    }

    @Test
    @DisplayName("validateConnection() wirft Exception wenn kein accessKey konfiguriert ist")
    void throwsWhenNoAccessKeyConfigured() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        Configuration configWithoutKey = new Configuration("TestFW",
                new DiveraConfig(null, null), List.of(), List.of(), List.of(), null, Map.of(), List.of());
        when(configService.getConfigForTenant("musterstadt")).thenReturn(configWithoutKey);

        assertThatThrownBy(() -> validator.validateConnection())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("accessKey");

        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("validateConnection() wirft Exception wenn Divera-API nicht erreichbar ist")
    void throwsWhenDiveraApiUnreachable() {
        when(configService.getKnownTenants()).thenReturn(List.of("musterstadt"));
        when(configService.getConfigForTenant("musterstadt")).thenReturn(config);
        when(client.pullAll(diveraConfig)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> validator.validateConnection())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    @DisplayName("validateConnection() macht nichts wenn keine Tenants bekannt sind")
    void doesNothingWhenNoTenantsKnown() {
        when(configService.getKnownTenants()).thenReturn(List.of());

        validator.validateConnection();

        verifyNoInteractions(client);
    }
}