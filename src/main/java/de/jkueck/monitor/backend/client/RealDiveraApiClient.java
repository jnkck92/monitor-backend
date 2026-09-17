package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@Profile("live")
@RequiredArgsConstructor
public class RealDiveraApiClient implements DiveraClient {

    private final TenantRestClientProvider restClients;

    private final MeterRegistry meterRegistry;

    @Override
    public DiveraResponse pullAll(DiveraConfig diveraConfig) {
        return timed("alarms", () -> restClients.forBaseUrl(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/alarms")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(DiveraResponse.class));
    }

    @Override
    public VehicleStatusGroupResponse pullVehicleStatus(DiveraConfig diveraConfig) {
        return timed("vehicle-status", () -> restClients.forBaseUrl(diveraConfig.baseUrl()).get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/pull/vehicle-status")
                        .queryParam("accesskey", diveraConfig.accessKey())
                        .build())
                .retrieve()
                .body(VehicleStatusGroupResponse.class));
    }

    private <T> T timed(String endpoint, Supplier<T> call) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String status = "success";
        try {
            return call.get();
        } catch (RuntimeException e) {
            status = "error";
            throw e;
        } finally {
            sample.stop(meterRegistry.timer("monitor.divera.api.latency", "endpoint", endpoint, "status", status));
            meterRegistry.counter("monitor.divera.api.requests", "endpoint", endpoint, "status", status).increment();
        }
    }
}