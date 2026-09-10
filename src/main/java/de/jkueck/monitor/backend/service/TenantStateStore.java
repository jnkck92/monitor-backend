package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantStateStore {

    private static final MonitorWebResponse INITIAL_STATE = new MonitorWebResponse("DEFAULT", "STANDBY", List.of(), List.of(), null, null, null);

    private final Map<String, MonitorWebResponse> stateByTenant = new ConcurrentHashMap<>();

    public MonitorWebResponse get(String tenant) {
        MonitorWebResponse state = stateByTenant.get(tenant);
        if (state == null) {
            throw new IllegalStateException("No monitor state available yet for tenant: " + tenant);
        }
        return state;
    }

    public MonitorWebResponse getOrInitial(String tenant) {
        return stateByTenant.getOrDefault(tenant, INITIAL_STATE);
    }

    public void put(String tenant, MonitorWebResponse state) {
        stateByTenant.put(tenant, state);
    }

    public Map<String, MonitorWebResponse> getAll() {
        return Map.copyOf(stateByTenant);
    }

}
