package de.jkueck.monitor.backend.client;

import de.jkueck.monitor.backend.config.DiveraProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantRestClientProvider {

    private final RestClient.Builder builderTemplate;

    private final DiveraProperties defaults;

    private final Map<String, RestClient> restClientsByBaseUrl = new ConcurrentHashMap<>();

    public TenantRestClientProvider(RestClient.Builder builderTemplate, DiveraProperties defaults) {
        this.builderTemplate = builderTemplate;
        this.defaults = defaults;
    }

    public RestClient forBaseUrl(String baseUrl) {
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : defaults.baseUrl();
        return restClientsByBaseUrl.computeIfAbsent(effectiveBaseUrl, this::build);
    }

    private RestClient build(String baseUrl) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(defaults.connectTimeout());
        requestFactory.setReadTimeout(defaults.readTimeout());

        return builderTemplate.clone()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

}
