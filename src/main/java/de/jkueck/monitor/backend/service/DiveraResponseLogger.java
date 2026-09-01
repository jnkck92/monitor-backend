package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
public class DiveraResponseLogger {

    private static final Logger responseLogger = LoggerFactory.getLogger("divera.response");

    private final ObjectMapper jsonObjectMapper;
    private final MonitorStateBuilder stateBuilder;
    private final ActiveAlarmResolver activeAlarmResolver;
    private final AtomicReference<Long> lastLoggedAlarmId = new AtomicReference<>(null);

    public void logIfChanged(DiveraResponse alarmResponse) {
        Optional<AlarmResponse> activeAlarm = activeAlarmResolver.find(alarmResponse);
        Long currentAlarmId = activeAlarm.map(AlarmResponse::id).orElse(null);
        Long previousAlarmId = lastLoggedAlarmId.get();

        if (!Objects.equals(currentAlarmId, previousAlarmId)) {
            lastLoggedAlarmId.set(currentAlarmId);
            if (currentAlarmId != null) {
                try {
                    responseLogger.info("ALARM DETECTED [id={}]: alarms={}",
                            currentAlarmId, jsonObjectMapper.writeValueAsString(alarmResponse));
                } catch (Exception e) {
                    LoggerFactory.getLogger(getClass()).error("Failed to serialize response for logging", e);
                }
            } else {
                responseLogger.info("ALARM ENDED (previous id={})", previousAlarmId);
            }
        }
    }
}