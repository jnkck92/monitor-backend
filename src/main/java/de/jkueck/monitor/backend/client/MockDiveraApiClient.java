package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Profile("dev")
public class MockDiveraApiClient implements DiveraClient {

    private final AtomicBoolean alarmActive = new AtomicBoolean(false);

    public void setAlarmActive(boolean active) {
        alarmActive.set(active);
    }

    public boolean isAlarmActive() {
        return alarmActive.get();
    }

    @Override
    public DiveraResponse pullAll(DiveraConfig diveraConfig) {
        if (!alarmActive.get()) {
            return new DiveraResponse(true, new DiveraResponse.Data(Map.of()));
        }
        return new DiveraResponse(
                true,
                new DiveraResponse.Data(
                        Map.of("123", new AlarmResponse(
                                123L, "F012 - Heckenbrand", "Brennt Hecke",
                                "Teststraße 1, 12345 Testort",
                                Instant.now().getEpochSecond(), false, true
                        ))
                )
        );
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        List<VehicleStatus> mockStatuses = List.of(
                new VehicleStatus(4716L, alarmActive.get() ? 3 : 2), // ELW
                new VehicleStatus(4714L, alarmActive.get() ? 4 : 1), // HLF
                new VehicleStatus(4715L, 1), // TLF
                new VehicleStatus(7185L, 1), // RW
                new VehicleStatus(7184L, 1), // SW
                new VehicleStatus(44882L, 1), // MTW
                new VehicleStatus(45764L, 1), // RTB
                new VehicleStatus(55884L, 1), // OBM
                new VehicleStatus(55885L, 1), // OBMV
                new VehicleStatus(55886L, 1), // ZF
                new VehicleStatus(86298L, 1) // ZFV
        );

        return new VehicleStatusGroupResponse(true, mockStatuses);
    }
}