package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.config.DiveraProperties;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Profile("live")
public class RealDiveraApiClient implements DiveraClient {

    private final RestClient restClient;
    private final DiveraProperties properties;

    public RealDiveraApiClient(DiveraProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).build();
    }

    @Override
    public DiveraResponse pullAll() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/alarms")
                        .queryParam("accesskey", properties.accessKey())
                        .build())
                .retrieve()
                .body(DiveraResponse.class);
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/pull/vehicle-status")
                        .queryParam("accesskey", properties.accessKey())
                        .build())
                .retrieve()
                .body(VehicleStatusGroupResponse.class);
    }
}