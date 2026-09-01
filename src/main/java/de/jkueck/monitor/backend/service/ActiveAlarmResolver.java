package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.divera.AlarmResponse;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ActiveAlarmResolver {

    public Optional<AlarmResponse> find(DiveraResponse response) {
        if (response.data() == null || response.data().items() == null) {
            return Optional.empty();
        }
        return response.data().items().values().stream()
                .filter(a -> a.closed() == null || !a.closed())
                .findFirst();
    }
}