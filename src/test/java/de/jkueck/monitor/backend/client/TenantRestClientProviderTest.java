package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.config.DiveraProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TenantRestClientProviderTest {

    private final DiveraProperties defaults =
            new DiveraProperties("https://default.example.com", 10000L, Duration.ofSeconds(5), Duration.ofSeconds(10));

    private final TenantRestClientProvider provider =
            new TenantRestClientProvider(RestClient.builder(), defaults);

    @Test
    void returnsSameInstanceForSameBaseUrl() {
        RestClient first = provider.forBaseUrl("https://tenant-a.example.com");
        RestClient second = provider.forBaseUrl("https://tenant-a.example.com");

        assertThat(first).isSameAs(second);
    }

    @Test
    void returnsDifferentInstancesForDifferentBaseUrls() {
        RestClient a = provider.forBaseUrl("https://tenant-a.example.com");
        RestClient b = provider.forBaseUrl("https://tenant-b.example.com");

        assertThat(a).isNotSameAs(b);
    }

    @Test
    void fallsBackToDefaultBaseUrlWhenNullOrBlank() {
        RestClient viaNull = provider.forBaseUrl(null);
        RestClient viaDefault = provider.forBaseUrl(defaults.baseUrl());

        assertThat(viaNull).isSameAs(viaDefault);
    }
}