package de.jkueck.monitor.backend.event;

import de.jkueck.monitor.backend.dto.response.MonitorWebResponse;
import org.springframework.context.ApplicationEvent;

public class MonitorStateChangedEvent extends ApplicationEvent {

    private final MonitorWebResponse state;

    public MonitorStateChangedEvent(Object source, MonitorWebResponse state) {
        super(source);
        this.state = state;
    }

    public MonitorWebResponse getState() {
        return state;
    }
}