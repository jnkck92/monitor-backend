package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantStateStoreTest {

    private final TenantStateStore store = new TenantStateStore();

    @Test
    void getThrowsWhenTenantUnknown() {
        assertThatThrownBy(() -> store.get("unbekannt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unbekannt");
    }

    @Test
    void getOrInitialReturnsDefaultStateWhenUnknown() {
        MonitorWebResponse state = store.getOrInitial("unbekannt");
        assertThat(state.mode()).isEqualTo("STANDBY");
    }

    @Test
    void putAndGetReturnsStoredState() {
        MonitorWebResponse state = new MonitorWebResponse("FW", "ALARM", List.of(), List.of(), null, null, null);
        store.put("musterstadt", state);

        assertThat(store.get("musterstadt")).isEqualTo(state);
    }

    @Test
    void getAllReturnsAllStoredTenants() {
        store.put("a", new MonitorWebResponse("A", "STANDBY", List.of(), List.of(), null, null, null));
        store.put("b", new MonitorWebResponse("B", "ALARM", List.of(), List.of(), null, null, null));

        assertThat(store.getAll()).containsKeys("a", "b");
    }

    @Test
    void getAllReturnsEmptyMapInitially() {
        assertThat(store.getAll()).isEmpty();
    }
}