package de.jkueck.monitor.backend.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import de.jkueck.monitor.backend.config.DiveraProperties;
import de.jkueck.monitor.backend.dto.response.divera.DiveraResponse;
import de.jkueck.monitor.backend.dto.response.divera.VehicleStatusGroupResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WireMockTest
class RealDiveraApiClientTest {

    private RealDiveraApiClient createClient(String baseUrl) {
        DiveraProperties properties = new DiveraProperties("test-key", baseUrl, 10000L);
        return new RealDiveraApiClient(properties);
    }

    @Test
    @DisplayName("pullAll() gibt DiveraResponse bei erfolgreicher Antwort zurück")
    void pullAllReturnsResponse(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .withQueryParam("accesskey", equalTo("test-key"))
                .willReturn(okJson("""
                        {
                          "success": true,
                          "data": {
                            "items": {}
                          }
                        }
                        """)));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());
        DiveraResponse response = client.pullAll();

        assertThat(response.success()).isTrue();
        assertThat(response.data().items()).isEmpty();
    }

    @Test
    @DisplayName("pullAll() parst aktiven Alarm korrekt")
    void pullAllParsesActiveAlarm(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .willReturn(okJson("""
                        {
                          "success": true,
                          "data": {
                            "items": {
                              "123": {
                                "id": 123,
                                "title": "F01 Kleinbrand",
                                "text": "Brennt Hecke",
                                "address": "Musterstr. 1",
                                "date": 1725192000,
                                "closed": false,
                                "priority": true
                              }
                            }
                          }
                        }
                        """)));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());
        DiveraResponse response = client.pullAll();

        assertThat(response.data().items()).hasSize(1);
        assertThat(response.data().items().get("123").title()).isEqualTo("F01 Kleinbrand");
        assertThat(response.data().items().get("123").address()).isEqualTo("Musterstr. 1");
        assertThat(response.data().items().get("123").closed()).isFalse();
    }

    @Test
    @DisplayName("pullVehicleStatus() gibt VehicleStatusGroupResponse zurück")
    void pullVehicleStatusReturnsResponse(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/pull/vehicle-status"))
                .withQueryParam("accesskey", equalTo("test-key"))
                .willReturn(okJson("""
                        {
                          "success": true,
                          "data": [
                            {"id": 4716, "fmsstatus": 2},
                            {"id": 4714, "fmsstatus": 1}
                          ]
                        }
                        """)));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());
        VehicleStatusGroupResponse response = client.pullVehicleStatus();

        assertThat(response.success()).isTrue();
        assertThat(response.data()).hasSize(2);
        assertThat(response.data().getFirst().id()).isEqualTo(4716L);
        assertThat(response.data().getFirst().fmsstatus()).isEqualTo(2);
    }

    @Test
    @DisplayName("pullAll() sendet accesskey als Query-Parameter")
    void pullAllSendsAccessKey(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .willReturn(okJson("""
                        {"success": true, "data": {"items": {}}}
                        """)));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());
        client.pullAll();

        verify(getRequestedFor(urlPathEqualTo("/v2/alarms"))
                .withQueryParam("accesskey", equalTo("test-key")));
    }

    @Test
    @DisplayName("pullAll() wirft Exception bei Server-Fehler")
    void pullAllThrowsOnServerError(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .willReturn(serverError()));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());

        assertThatThrownBy(client::pullAll).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("pullAll() wirft Exception bei Timeout")
    void pullAllThrowsOnTimeout(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .willReturn(ok().withFixedDelay(15000))); // 15s > 10s readTimeout

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());

        assertThatThrownBy(client::pullAll)
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    @DisplayName("pullAll() behandelt 401 Unauthorized")
    void pullAllHandlesUnauthorized(WireMockRuntimeInfo wm) {
        stubFor(get(urlPathEqualTo("/v2/alarms"))
                .willReturn(unauthorized()));

        RealDiveraApiClient client = createClient(wm.getHttpBaseUrl());

        assertThatThrownBy(client::pullAll).isInstanceOf(Exception.class);
    }
}