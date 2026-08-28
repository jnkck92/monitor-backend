package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.service.ConfigurationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ConfigurationController {

    private final ConfigurationService configService;

    @GetMapping("/api/v1/configuration")
    public Configuration getConfiguration() {
        return configService.getConfig();
    }

    @PostMapping("/api/v1/configuration/reload")
    public Configuration reloadConfiguration() {
        configService.reload();
        return configService.getConfig();
    }

}