package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.NewsWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsListResponse;
import de.jkueck.monitor.backend.service.ConfigurationProvider;
import de.jkueck.monitor.backend.service.NewsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NewsController.class)
class NewsControllerTest {

    private static final String TENANT = "musterstadt";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiveraClient client;

    @MockitoBean
    private ConfigurationProvider configService;

    @MockitoBean
    private NewsService newsService;

    @Test
    @DisplayName("GET /api/v1/news gibt Mitteilungen für den angegebenen Tenant zurück")
    void getNewsReturnsNews() throws Exception {
        DiveraConfig diveraConfig = new DiveraConfig("test-key", "https://app.divera247.com/api");
        Configuration config = new Configuration("TestFW", diveraConfig, List.of(), List.of(), List.of(), null, Map.of(), List.of());
        NewsListResponse newsListResponse = new NewsListResponse(true, new NewsListResponse.Data(Map.of(), List.of()));

        when(configService.getConfigForTenant(TENANT)).thenReturn(config);
        when(client.pullNews(diveraConfig)).thenReturn(newsListResponse);
        when(newsService.buildNewsList(newsListResponse)).thenReturn(List.of(
                new NewsWebResponse(1L, "Dienstanweisung", "Beschreibung", "Raum 1.23", null, false, true)
        ));

        mockMvc.perform(get("/api/v1/news").header("X-Tenant", TENANT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Dienstanweisung"))
                .andExpect(jsonPath("$[0].address").value("Raum 1.23"));
    }

    @Test
    @DisplayName("GET /api/v1/news ohne X-Tenant Header liefert 400")
    void getNewsWithoutTenantHeaderReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/news"))
                .andExpect(status().isBadRequest());
    }
}