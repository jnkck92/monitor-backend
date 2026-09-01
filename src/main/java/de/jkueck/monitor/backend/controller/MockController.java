package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.MockDiveraApiClient;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import de.jkueck.monitor.backend.client.MockDiveraApiClient;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
@RequestMapping("/mock/alarm")
@Tag(name = "Mock", description = "Mock endpoints for testing (dev profile only)")
public class MockController {

    private final MockDiveraApiClient mockClient;

    private final MonitorPollingService pollingService;

    @Operation(
            summary = "Activate mock alarm",
            description = "Sets the mock alarm to active and triggers an immediate poll to update the monitor state"
    )
    @ApiResponse(responseCode = "200", description = "Alarm activated")
    @PostMapping("/on")
    public String alarmOn() {
        mockClient.setAlarmActive(true);
        pollingService.poll();
        return "Alarm active";
    }

    @Operation(
            summary = "Deactivate mock alarm",
            description = "Sets the mock alarm to inactive and triggers an immediate poll to return to standby mode"
    )
    @ApiResponse(responseCode = "200", description = "Standby activated")
    @PostMapping("/off")
    public String alarmOff() {
        mockClient.setAlarmActive(false);
        pollingService.poll();
        return "Standby active";
    }
}