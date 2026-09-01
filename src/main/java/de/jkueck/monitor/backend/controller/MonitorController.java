package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import de.jkueck.monitor.backend.service.MonitorPollingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MonitorController {

    private final MonitorPollingService pollingService;

    @GetMapping("/api/v1/monitor/status")
    public MonitorWebResponse getStatus() {
        return pollingService.getCurrentState().get();
    }

}