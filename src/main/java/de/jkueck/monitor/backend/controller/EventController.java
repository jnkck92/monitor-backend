package de.jkueck.monitor.backend.controller;

import de.jkueck.monitor.backend.client.DiveraClient;
import de.jkueck.monitor.backend.dto.configuration.Configuration;
import de.jkueck.monitor.backend.dto.configuration.DiveraConfig;
import de.jkueck.monitor.backend.dto.response.EventWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.EventsResponse;
import de.jkueck.monitor.backend.service.ConfigurationProvider;
import de.jkueck.monitor.backend.service.DiveraConfigValidator;
import de.jkueck.monitor.backend.service.EventService;
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
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Endpoints for retrieving Divera events/Termine")
public class EventController {

    private final DiveraClient client;
    private final ConfigurationProvider configService;
    private final EventService eventService;

    @Operation(
            summary = "Get current events",
            description = "Returns all non-archived events (Termine) for the given tenant"
    )
    @ApiResponse(responseCode = "200", description = "Current events",
            content = @Content(schema = @Schema(implementation = EventWebResponse.class)))
    @GetMapping
    public List<EventWebResponse> getEvents(@RequestHeader("X-Tenant") String tenant) {
        Configuration config = configService.getConfigForTenant(tenant);
        DiveraConfig diveraConfig = config.divera();
        DiveraConfigValidator.requireAccessKey(tenant, diveraConfig);

        EventsResponse eventsResponse = client.pullEvents(diveraConfig);
        return eventService.buildEventList(eventsResponse);
    }
}