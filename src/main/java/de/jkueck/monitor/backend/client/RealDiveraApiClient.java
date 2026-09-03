package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.config.DiveraProperties;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Profile("live")
public class RealDiveraApiClient implements DiveraClient {

    private final DiveraProperties defaults;
    private final Map<String, RestClient> restClientsByBaseUrl = new ConcurrentHashMap<>();

    public RealDiveraApiClient(DiveraProperties defaults) {
        this.defaults = defaults;
    }

    private RestClient restClientFor(String baseUrl) {
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : defaults.baseUrl();
        return restClientsByBaseUrl.computeIfAbsent(effectiveBaseUrl, this::buildRestClient);
    }

    private RestClient buildRestClient(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public DiveraResponse pullAll(DiveraConfig diveraConfig) {
        return restClientFor(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/alarms")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(DiveraResponse.class);
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        return restClientFor(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/pull/vehicle-status")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(VehicleStatusGroupResponse.class);
    }
}