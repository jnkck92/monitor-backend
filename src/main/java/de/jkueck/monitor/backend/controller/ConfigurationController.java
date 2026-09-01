package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.service.ConfigurationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/configuration")
@Tag(name = "Configuration", description = "Endpoints for managing the monitor configuration")
public class ConfigurationController {

    private final ConfigurationService configService;

    @Operation(
            summary = "Get current configuration",
            description = "Returns the currently loaded configuration including departments, vehicles, persons, rules and statuses"
    )
    @ApiResponse(responseCode = "200", description = "Current configuration",
            content = @Content(schema = @Schema(implementation = Configuration.class)))
    @GetMapping
    public Configuration getConfiguration() {
        return configService.getConfig();
    }

    @Operation(
            summary = "Reload configuration from file",
            description = "Forces a reload of the configuration from the YAML file and returns the newly loaded configuration"
    )
    @ApiResponse(responseCode = "200", description = "Reloaded configuration",
            content = @Content(schema = @Schema(implementation = Configuration.class)))
    @PostMapping("/reload")
    public Configuration reloadConfiguration() {
        configService.reload();
        return configService.getConfig();
    }
}