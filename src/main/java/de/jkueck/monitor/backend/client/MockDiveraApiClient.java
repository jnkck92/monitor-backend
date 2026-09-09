package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatus;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
                        Map.of("123",
                                new AlarmResponse(
                                        123L, "H 052 - Türnotöffnung",
                                        "Brennt ein Wahlplakat an einer Laterne",
                                        "Kiepelbergstraße, 27721 Ritterhude Ritterhude",
                                        Instant.now().minus(10, ChronoUnit.MINUTES).getEpochSecond(),
                                        false,
                                        true
                                )
                        )
                )
        );
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        List<VehicleStatus> mockStatuses = List.of(
                new VehicleStatus(4716L, alarmActive.get() ? 4 : 2), // ELW
                new VehicleStatus(4714L, alarmActive.get() ? 3 : 2), // HLF
                new VehicleStatus(4715L, alarmActive.get() ? 3 : 2), // TLF
                new VehicleStatus(7185L, 2), // RW
                new VehicleStatus(7184L, 2), // SW
                new VehicleStatus(44882L, 2), // MTW
                new VehicleStatus(45764L, 2), // RTB
                new VehicleStatus(55884L, alarmActive.get() ? 4 : 2), // OBM
                new VehicleStatus(55885L, alarmActive.get() ? 4 : 2), // OBMV
                new VehicleStatus(55886L, alarmActive.get() ? 4 : 2), // ZF
                new VehicleStatus(86298L, alarmActive.get() ? 4 : 2) // ZFV
        );

        return new VehicleStatusGroupResponse(true, mockStatuses);
    }
}