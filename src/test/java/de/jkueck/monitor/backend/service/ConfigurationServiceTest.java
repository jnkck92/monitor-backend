package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.exception.ConfigurationLoadException;
import de.jkueck.monitor.backend.exception.UnknownTenantException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationServiceTest {

    private final ObjectMapper yamlMapper = YAMLMapper.builder().build();

    @TempDir
    Path tempDir;

    private ConfigurationService createService(List<String> tenants) {
        return new ConfigurationService(new ConfigurationProperties(tempDir.toString(), tenants), yamlMapper, new SimpleMeterRegistry());
    }

    private void writeConfigForTenant(String tenant, String yaml) throws IOException {
        Path tenantDir = tempDir.resolve(tenant);
        Files.createDirectories(tenantDir);
        Files.writeString(tenantDir.resolve("instance-config.yaml"), yaml);
    }

    private static final String VALID_CONFIG = """
            departmentName: TestFW
            divera:
              accessKey: "test-key"
              baseUrl: "https://www.divera247.com"
            persons: []
            vehicles:
              - id: v1
                name: LF20
                shortName: LF
                type: Löschfahrzeug
                ric: "15/48-4"
                diveraId: 100
            defaultOrder:
              - v1
            commandContact: "ELW 15/11-4"
            statuses:
              "2":
                label: "BEREIT"
                color: "#00ff00"
            ruleGroups:
              - category: F
                label: Brand
                color: "#ff0000"
                rules:
                  - label: Kleinbrand
                    keywords: ["F01"]
                    vehicleOrder: [v1]
            """;

    @Test
    @DisplayName("getConfigForTenant() lädt gültige YAML korrekt")
    void loadConfigSuccessfully() throws IOException {
        writeConfigForTenant("musterstadt", VALID_CONFIG);
        ConfigurationService service = createService(List.of("musterstadt"));

        Configuration config = service.getConfigForTenant("musterstadt");

        assertThat(config.departmentName()).isEqualTo("TestFW");
        assertThat(config.divera().accessKey()).isEqualTo("test-key");
        assertThat(config.vehicles()).hasSize(1);
        assertThat(config.vehicles().getFirst().id()).isEqualTo("v1");
        assertThat(config.ruleGroups()).hasSize(1);
        assertThat(config.statuses()).containsKey("2");
    }

    @Test
    @DisplayName("getConfigForTenant() wirft Exception wenn Tenant-Verzeichnis nicht existiert")
    void loadConfigThrowsWhenTenantNotFound() {
        ConfigurationService service = createService(List.of());

        assertThatThrownBy(() -> service.getConfigForTenant("unbekannt"))
                .isInstanceOf(UnknownTenantException.class)
                .hasMessageContaining("Unknown tenant");
    }

    @Test
    @DisplayName("getConfigForTenant() wirft Exception bei ungültiger YAML")
    void loadConfigThrowsOnInvalidYaml() throws IOException {
        writeConfigForTenant("kaputt", "{{invalid yaml content!!");
        ConfigurationService service = createService(List.of("kaputt"));

        assertThatThrownBy(() -> service.getConfigForTenant("kaputt"))
                .isInstanceOf(ConfigurationLoadException.class);
    }

    @Test
    @DisplayName("getConfigForTenant() cached die Konfiguration")
    void getConfigCachesResult() throws IOException {
        writeConfigForTenant("musterstadt", VALID_CONFIG);
        ConfigurationService service = createService(List.of("musterstadt"));

        Configuration first = service.getConfigForTenant("musterstadt");
        Configuration second = service.getConfigForTenant("musterstadt");

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("reloadTenant() lädt die Konfiguration neu von der Datei")
    void reloadTenantUpdatesConfig() throws IOException {
        writeConfigForTenant("musterstadt", VALID_CONFIG);
        ConfigurationService service = createService(List.of("musterstadt"));

        assertThat(service.getConfigForTenant("musterstadt").departmentName()).isEqualTo("TestFW");

        writeConfigForTenant("musterstadt", VALID_CONFIG.replace("TestFW", "Neue Wehr"));
        service.reloadTenant("musterstadt");

        assertThat(service.getConfigForTenant("musterstadt").departmentName()).isEqualTo("Neue Wehr");
    }

    @Test
    @DisplayName("reloadAll() leert den Cache und lädt alle bekannten Tenants neu")
    void reloadAllClearsCacheAndReloadsKnownTenants() throws IOException {
        writeConfigForTenant("tenant-a", VALID_CONFIG);
        writeConfigForTenant("tenant-b", VALID_CONFIG.replace("TestFW", "Andere Wehr"));
        ConfigurationService service = createService(List.of("tenant-a", "tenant-b"));

        Configuration a1 = service.getConfigForTenant("tenant-a");
        Configuration b1 = service.getConfigForTenant("tenant-b");

        service.reloadAll();

        Configuration a2 = service.getConfigForTenant("tenant-a");
        Configuration b2 = service.getConfigForTenant("tenant-b");

        assertThat(a2).isNotSameAs(a1);
        assertThat(b2).isNotSameAs(b1);
        assertThat(a2.departmentName()).isEqualTo("TestFW");
        assertThat(b2.departmentName()).isEqualTo("Andere Wehr");
    }

    @Test
    @DisplayName("reloadAll() wirft Exception wenn ein bekannter Tenant keine gültige Config hat")
    void reloadAllThrowsWhenKnownTenantConfigMissing() throws IOException {
        writeConfigForTenant("tenant-a", VALID_CONFIG);
        // tenant-b ist als bekannt gelistet, hat aber keine Config-Datei
        ConfigurationService service = createService(List.of("tenant-a", "tenant-b"));

        assertThatThrownBy(service::reloadAll)
                .isInstanceOf(UnknownTenantException.class)
                .hasMessageContaining("tenant-b");
    }

    @Test
    @DisplayName("verschiedene Tenants liefern unterschiedliche Konfigurationen")
    void differentTenantsHaveIsolatedConfigs() throws IOException {
        writeConfigForTenant("tenant-a", VALID_CONFIG);
        writeConfigForTenant("tenant-b", VALID_CONFIG
                .replace("TestFW", "Andere Wehr")
                .replace("test-key", "other-key"));
        ConfigurationService service = createService(List.of("tenant-a", "tenant-b"));

        Configuration a = service.getConfigForTenant("tenant-a");
        Configuration b = service.getConfigForTenant("tenant-b");

        assertThat(a.departmentName()).isEqualTo("TestFW");
        assertThat(a.divera().accessKey()).isEqualTo("test-key");
        assertThat(b.departmentName()).isEqualTo("Andere Wehr");
        assertThat(b.divera().accessKey()).isEqualTo("other-key");
    }

    @Test
    @DisplayName("getConfigForTenant() parst Personen korrekt")
    void loadConfigParsesPersons() throws IOException {
        String yaml = """
                departmentName: TestFW
                divera:
                  accessKey: "test-key"
                persons:
                  - id: obm
                    name: OrtsBm
                    shortName: OBM
                    type: Ortsbrandmeister
                    ric: "15/03-4"
                    diveraId: 55884
                vehicles: []
                defaultOrder: []
                statuses: {}
                ruleGroups: []
                """;
        writeConfigForTenant("musterstadt", yaml);
        ConfigurationService service = createService(List.of("musterstadt"));

        Configuration config = service.getConfigForTenant("musterstadt");
        assertThat(config.persons()).hasSize(1);
        assertThat(config.persons().getFirst().shortName()).isEqualTo("OBM");
        assertThat(config.persons().getFirst().diveraId()).isEqualTo(55884L);
    }

    @Test
    @DisplayName("getConfigForTenant() parst hint in Rules korrekt")
    void loadConfigParsesHint() throws IOException {
        String yaml = """
                departmentName: TestFW
                divera:
                  accessKey: "test-key"
                persons: []
                vehicles: []
                defaultOrder: []
                statuses: {}
                ruleGroups:
                  - category: F
                    label: Brand
                    color: "#ff0000"
                    rules:
                      - label: Zimmerbrand
                        keywords: ["F01"]
                        vehicleOrder: []
                        hint: "Atemschutz bereitstellen"
                """;
        writeConfigForTenant("musterstadt", yaml);
        ConfigurationService service = createService(List.of("musterstadt"));

        Configuration config = service.getConfigForTenant("musterstadt");
        assertThat(config.ruleGroups().getFirst().rules().getFirst().hint())
                .isEqualTo("Atemschutz bereitstellen");
    }

    @Test
    @DisplayName("getKnownTenants() gibt die konfigurierte Tenant-Liste zurück")
    void getKnownTenantsReturnsConfiguredList() {
        ConfigurationService service = createService(List.of("tenant-a", "tenant-b"));

        assertThat(service.getKnownTenants()).containsExactly("tenant-a", "tenant-b");
    }

    @Test
    @DisplayName("getKnownTenants() gibt leere Liste zurück wenn keine Tenants konfiguriert sind")
    void getKnownTenantsReturnsEmptyListWhenNoneConfigured() {
        ConfigurationService service = createService(List.of());

        assertThat(service.getKnownTenants()).isEmpty();
    }

    @Test
    @DisplayName("getConfigForTenant() lehnt Path-Traversal-Versuche ab")
    void rejectsPathTraversalAttempt() {
        ConfigurationService service = createService(List.of("musterstadt"));

        assertThatThrownBy(() -> service.getConfigForTenant("../../etc/passwd")).isInstanceOf(UnknownTenantException.class);
    }

    @Test
    @DisplayName("getConfigForTenant() liefert aussagekräftige Fehlermeldung bei falschem Feldtyp")
    void loadConfigThrowsWithDescriptiveMessageOnTypeMismatch() throws IOException {
        String yamlWithWrongType = """
            departmentName: TestFW
            divera:
              accessKey: "test-key"
            persons: []
            vehicles: "das sollte eine Liste sein, nicht ein String"
            defaultOrder: []
            statuses: {}
            ruleGroups: []
            """;
        writeConfigForTenant("kaputt-typ", yamlWithWrongType);
        ConfigurationService service = createService(List.of("kaputt-typ"));

        assertThatThrownBy(() -> service.getConfigForTenant("kaputt-typ"))
                .isInstanceOf(ConfigurationLoadException.class)
                .hasMessageContaining("kaputt-typ");
    }

    @Test
    @DisplayName("getConfigForTenant() liefert aussagekräftige Fehlermeldung bei nicht lesbarer Datei")
    void loadConfigThrowsWithDescriptiveMessageOnUnreadableFile() throws IOException {
        writeConfigForTenant("nicht-lesbar", VALID_CONFIG);
        Path configFile = tempDir.resolve("nicht-lesbar").resolve("instance-config.yaml");
        // Datei für den aktuellen Benutzer unlesbar machen (funktioniert nicht unter allen OS/CI-Umgebungen zuverlässig,
        // ggf. per @DisabledOnOs(WINDOWS) einschränken oder mit einem gemockten ObjectMapper simulieren)
        configFile.toFile().setReadable(false);
        ConfigurationService service = createService(List.of("nicht-lesbar"));

        try {
            assertThatThrownBy(() -> service.getConfigForTenant("nicht-lesbar"))
                    .isInstanceOf(ConfigurationLoadException.class);
        } finally {
            configFile.toFile().setReadable(true); // Aufräumen für nachfolgende Tests/CI
        }
    }

}