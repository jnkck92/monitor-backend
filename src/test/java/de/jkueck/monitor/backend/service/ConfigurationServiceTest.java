package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.config.ConfigurationProperties;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigurationServiceTest {

    private final ObjectMapper yamlMapper = YAMLMapper.builder().build();

    @TempDir
    Path tempDir;

    private ConfigurationService createService(String path) {
        return new ConfigurationService(new ConfigurationProperties(path), yamlMapper);
    }

    private Path writeConfig(String yaml) throws IOException {
        Path file = tempDir.resolve("test-config.yaml");
        Files.writeString(file, yaml);
        return file;
    }

    private static final String VALID_CONFIG = """
            departmentName: TestFW
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
    @DisplayName("loadConfig() lädt gültige YAML korrekt")
    void loadConfigSuccessfully() throws IOException {
        Path file = writeConfig(VALID_CONFIG);
        ConfigurationService service = createService(file.toString());

        service.loadConfig();

        Configuration config = service.getConfig();
        assertThat(config.departmentName()).isEqualTo("TestFW");
        assertThat(config.vehicles()).hasSize(1);
        assertThat(config.vehicles().getFirst().id()).isEqualTo("v1");
        assertThat(config.ruleGroups()).hasSize(1);
        assertThat(config.statuses()).containsKey("2");
    }

    @Test
    @DisplayName("loadConfig() wirft Exception wenn Datei nicht existiert")
    void loadConfigThrowsWhenFileNotFound() {
        ConfigurationService service = createService("/non/existent/path.yaml");

        assertThatThrownBy(service::loadConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("loadConfig() wirft Exception bei ungültiger YAML")
    void loadConfigThrowsOnInvalidYaml() throws IOException {
        Path file = writeConfig("{{invalid yaml content!!");
        ConfigurationService service = createService(file.toString());

        assertThatThrownBy(service::loadConfig)
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("getConfig() wirft Exception wenn noch nicht geladen")
    void getConfigThrowsWhenNotLoaded() {
        ConfigurationService service = createService("/dummy");

        assertThatThrownBy(service::getConfig)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not been loaded");
    }

    @Test
    @DisplayName("reload() aktualisiert die Konfiguration")
    void reloadUpdatesConfig() throws IOException {
        Path file = writeConfig(VALID_CONFIG);
        ConfigurationService service = createService(file.toString());
        service.loadConfig();

        assertThat(service.getConfig().departmentName()).isEqualTo("TestFW");

        // Datei überschreiben
        Files.writeString(file, VALID_CONFIG.replace("TestFW", "Neue Wehr"));
        service.reload();

        assertThat(service.getConfig().departmentName()).isEqualTo("Neue Wehr");
    }

    @Test
    @DisplayName("loadConfig() parst Personen korrekt")
    void loadConfigParsesPersons() throws IOException {
        String yaml = """
                departmentName: TestFW
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
        Path file = writeConfig(yaml);
        ConfigurationService service = createService(file.toString());
        service.loadConfig();

        assertThat(service.getConfig().persons()).hasSize(1);
        assertThat(service.getConfig().persons().getFirst().shortName()).isEqualTo("OBM");
        assertThat(service.getConfig().persons().getFirst().diveraId()).isEqualTo(55884L);
    }

    @Test
    @DisplayName("loadConfig() parst hint in Rules korrekt")
    void loadConfigParsesHint() throws IOException {
        String yaml = """
                departmentName: TestFW
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
        Path file = writeConfig(yaml);
        ConfigurationService service = createService(file.toString());
        service.loadConfig();

        assertThat(service.getConfig().ruleGroups().getFirst().rules().getFirst().hint())
                .isEqualTo("Atemschutz bereitstellen");
    }
}