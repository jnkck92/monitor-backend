package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/monitor")
@Tag(name = "Monitor", description = "Endpoints for retrieving the current monitor state")
public class MonitorController {

    private final MonitorPollingService pollingService;

    @Operation(
            summary = "Get current monitor status",
            description = "Returns the current monitor state including mode (STANDBY/ALARM), vehicles, persons and alarm details if active"
    )
    @ApiResponse(responseCode = "200", description = "Current monitor state",
            content = @Content(schema = @Schema(implementation = MonitorWebResponse.class)))
    @GetMapping("/status")
    public MonitorWebResponse getStatus() {
        return pollingService.getCurrentState().get();
    }
}