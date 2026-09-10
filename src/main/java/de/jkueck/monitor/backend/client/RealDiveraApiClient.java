package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("live")
@RequiredArgsConstructor
public class RealDiveraApiClient implements DiveraClient {

    private final TenantRestClientProvider restClients;

    @Override
    public DiveraResponse pullAll(DiveraConfig diveraConfig) {
        return restClients.forBaseUrl(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/alarms")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(DiveraResponse.class);
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        return restClients.forBaseUrl(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/pull/vehicle-status")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(VehicleStatusGroupResponse.class);
    }
}