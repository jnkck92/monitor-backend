package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.client.GeocodingClient;
import de.jkueck.monitor.backend.dto.response.Coordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GeocodingService {

    private final GeocodingClient geocodingClient;

    private final Map<String, Optional<Coordinates>> cache = new ConcurrentHashMap<>();

    public Optional<Coordinates> geocode(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }

        String cleaned = cleanAddress(address);
        return cache.computeIfAbsent(cleaned, this::lookup);
    }

    private Optional<Coordinates> lookup(String cleanedAddress) {
        return geocodingClient.search(cleanedAddress)
                .map(result -> new Coordinates(Double.parseDouble(result.lat()), Double.parseDouble(result.lon())));
    }

    private String cleanAddress(String address) {
        String[] words = address.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        String previous = null;
        for (String word : words) {
            if (!word.equalsIgnoreCase(previous)) {
                result.append(word).append(' ');
            }
            previous = word;
        }
        return result.toString().trim();
    }
}