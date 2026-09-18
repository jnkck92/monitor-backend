package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.config.GeocodingProperties;
import de.jkueck.monitor.backend.dto.response.geocoding.NominatimResultResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Optional;

@Component
@Profile("live")
public class RealGeocodingClient implements GeocodingClient {

    private final RestClient restClient;
    private final MeterRegistry meterRegistry;

    public RealGeocodingClient(RestClient.Builder builderTemplate, GeocodingProperties properties, MeterRegistry meterRegistry) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        this.restClient = builderTemplate.clone()
                .baseUrl(properties.baseUrl())
                .defaultHeader("User-Agent", properties.userAgent())
                .requestFactory(requestFactory)
                .build();
        this.meterRegistry = meterRegistry;
    }

    @Override
    public Optional<NominatimResultResponse> search(String address) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = "success";
        try {
            List<NominatimResultResponse> results = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("q", address)
                            .queryParam("format", "json")
                            .queryParam("limit", 1)
                            .build())
                    .retrieve()
                    .body(new org.springframework.core.ParameterizedTypeReference<List<NominatimResultResponse>>() {
                    });

            return results == null || results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (RuntimeException e) {
            status = "error";
            return Optional.empty();
        } finally {
            sample.stop(meterRegistry.timer("monitor.geocoding.api.latency", "status", status));
            meterRegistry.counter("monitor.geocoding.api.requests", "status", status).increment();
        }
    }

}