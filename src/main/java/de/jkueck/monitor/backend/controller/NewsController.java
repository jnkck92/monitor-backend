package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.NewsWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsListResponse;
import de.jkueck.monitor.backend.service.ConfigurationProvider;
import de.jkueck.monitor.backend.service.DiveraConfigValidator;
import de.jkueck.monitor.backend.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/news")
@Tag(name = "News", description = "Endpoints for retrieving Divera news/Mitteilungen")
public class NewsController {

    private final DiveraClient client;
    private final ConfigurationProvider configService;
    private final NewsService newsService;

    @Operation(
            summary = "Get current news",
            description = "Returns all non-archived news (Mitteilungen) for the given tenant"
    )
    @ApiResponse(responseCode = "200", description = "Current news",
            content = @Content(schema = @Schema(implementation = NewsWebResponse.class)))
    @GetMapping
    public List<NewsWebResponse> getNews(@RequestHeader("X-Tenant") String tenant) {
        Configuration config = configService.getConfigForTenant(tenant);
        DiveraConfig diveraConfig = config.divera();
        DiveraConfigValidator.requireAccessKey(tenant, diveraConfig);

        NewsListResponse newsListResponse = client.pullNews(diveraConfig);
        return newsService.buildNewsList(newsListResponse);
    }
}