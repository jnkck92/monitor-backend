package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.Rule;
import de.jkueck.monitor.backend.dto.response.UnitWebResponse;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class VehicleOrderBuilder {

    public List<UnitWebResponse> buildOrderedList(List<UnitWebResponse> allVehicles, Rule rule, Configuration configuration) {
        Map<String, UnitWebResponse> vehicleIndex = allVehicles.stream()
                .collect(Collectors.toMap(UnitWebResponse::id, v -> v));

        Set<String> alertedIds = new LinkedHashSet<>(rule.vehicleOrder());

        List<UnitWebResponse> alerted = alertedIds.stream()
                .map(vehicleIndex::get)
                .filter(Objects::nonNull)
                .map(v -> withAlerted(v, true))
                .toList();

        List<String> remainingOrder = resolveRemainingOrder(rule, configuration);

        List<UnitWebResponse> nonAlerted = remainingOrder.stream()
                .filter(id -> !alertedIds.contains(id))
                .map(vehicleIndex::get)
                .filter(Objects::nonNull)
                .map(v -> withAlerted(v, false))
                .toList();

        List<UnitWebResponse> result = new ArrayList<>(alerted.size() + nonAlerted.size());
        result.addAll(alerted);
        result.addAll(nonAlerted);
        return result;
    }

    private static UnitWebResponse withAlerted(UnitWebResponse v, boolean alerted) {
        return new UnitWebResponse(v.id(), v.name(), v.callSign(), alerted, v.radioStatus());
    }

    private static List<String> resolveRemainingOrder(Rule rule, Configuration configuration) {
        if (rule.remainingOrder() != null && !rule.remainingOrder().isEmpty()) {
            return rule.remainingOrder();
        }
        return configuration.defaultOrder();
    }

}