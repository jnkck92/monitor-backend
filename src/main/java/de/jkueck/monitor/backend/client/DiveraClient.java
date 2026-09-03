package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;

public interface DiveraClient {

    DiveraResponse pullAll(DiveraConfig diveraConfig);

    VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig);

}
