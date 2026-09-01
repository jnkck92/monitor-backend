package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiveraResponseLoggerTest {

    @Mock
    private ObjectMapper jsonObjectMapper;

    @Mock
    private ActiveAlarmResolver activeAlarmResolver;

    @InjectMocks
    private DiveraResponseLogger responseLogger;

    private DiveraResponse noAlarmResponse() {
        return new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
    }

    private DiveraResponse alarmResponse(Long id, String title) {
        AlarmResponse alarm = new AlarmResponse(id, title, null, null, null, false, false);
        return new DiveraResponse(true, new DiveraResponse.Data(Map.of(id.toString(), alarm)));
    }

    @Test
    void logIfChanged_doesNotThrowOnNoAlarm() {
        DiveraResponse response = noAlarmResponse();
        when(activeAlarmResolver.find(response)).thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(() -> responseLogger.logIfChanged(response));
    }

    @Test
    void logIfChanged_doesNotThrowOnNewAlarm() throws Exception {
        DiveraResponse response = alarmResponse(1L, "B2 Brand");
        when(activeAlarmResolver.find(response))
                .thenReturn(Optional.of(response.data().items().get("1")));
        when(jsonObjectMapper.writeValueAsString(any())).thenReturn("{}");

        assertThatNoException().isThrownBy(() -> responseLogger.logIfChanged(response));
    }

    @Test
    void logIfChanged_detectsAlarmChange() throws Exception {
        // Erster Alarm
        DiveraResponse first = alarmResponse(1L, "B2 Brand");
        when(activeAlarmResolver.find(first))
                .thenReturn(Optional.of(first.data().items().get("1")));
        when(jsonObjectMapper.writeValueAsString(any())).thenReturn("{}");
        responseLogger.logIfChanged(first);

        // Zweiter Alarm mit anderer ID
        DiveraResponse second = alarmResponse(2L, "THL1 VU");
        when(activeAlarmResolver.find(second))
                .thenReturn(Optional.of(second.data().items().get("2")));

        assertThatNoException().isThrownBy(() -> responseLogger.logIfChanged(second));
    }

    @Test
    void logIfChanged_handlesSerializationError() throws Exception {
        DiveraResponse response = alarmResponse(1L, "B2 Brand");
        when(activeAlarmResolver.find(response))
                .thenReturn(Optional.of(response.data().items().get("1")));
        when(jsonObjectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("serialization error"));

        assertThatNoException().isThrownBy(() -> responseLogger.logIfChanged(response));
    }
}