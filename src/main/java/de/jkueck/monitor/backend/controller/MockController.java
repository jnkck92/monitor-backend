package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.MockDiveraApiClient;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("dev")
@RequiredArgsConstructor
public class MockController {

    private final MockDiveraApiClient mockClient;

    private final MonitorPollingService pollingService;

    @PostMapping("/mock/alarm/on")
    public String alarmOn() {
        mockClient.setAlarmActive(true);
        pollingService.poll();
        return "Alarm aktiviert";
    }

    @PostMapping("/mock/alarm/off")
    public String alarmOff() {
        mockClient.setAlarmActive(false);
        pollingService.poll();
        return "Standby aktiviert";
    }
}