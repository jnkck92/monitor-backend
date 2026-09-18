package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.NewsWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsListResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    private static final int MAX_NEWS = 4;

    public List<NewsWebResponse> buildNewsList(NewsListResponse newsListResponse) {
        if (newsListResponse == null || newsListResponse.data() == null || newsListResponse.data().items() == null) {
            return List.of();
        }

        Map<String, NewsResponse> items = newsListResponse.data().items();

        return items.values().stream()
                .filter(this::isVisible)
                .sorted(Comparator.comparing(NewsResponse::date, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_NEWS)
                .map(this::toWebResponse)
                .toList();
    }

    private boolean isVisible(NewsResponse news) {
        return news != null && !Boolean.TRUE.equals(news.deleted());
    }

    private NewsWebResponse toWebResponse(NewsResponse news) {
        Instant timestamp = news.date() != null ? Instant.ofEpochSecond(news.date()) : null;
        return new NewsWebResponse(news.id(), news.title(), news.text(), news.address(),
                timestamp, news.survey(), news.answerable());
    }
}