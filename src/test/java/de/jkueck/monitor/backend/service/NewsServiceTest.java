package de.jkueck.monitor.backend.service;

import de.jkueck.monitor.backend.dto.response.NewsWebResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsListResponse;
import de.jkueck.monitor.backend.dto.response.divera.NewsResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NewsServiceTest {

    private final NewsService service = new NewsService();

    @Test
    void buildNewsListReturnsEmptyListWhenResponseIsNull() {
        assertThat(service.buildNewsList(null)).isEmpty();
    }

    @Test
    void buildNewsListReturnsEmptyListWhenDataIsNull() {
        NewsListResponse response = new NewsListResponse(true, null);

        assertThat(service.buildNewsList(response)).isEmpty();
    }

    @Test
    void buildNewsListMapsFieldsCorrectly() {
        long epochSeconds = 1725192000L;
        NewsResponse news = new NewsResponse(1L, "ext-1", "Dienstanweisung", "Beschreibung", "Raum 1.23",
                epochSeconds, true, false, false);
        NewsListResponse response = new NewsListResponse(true, new NewsListResponse.Data(Map.of("1", news), List.of(1L)));

        List<NewsWebResponse> result = service.buildNewsList(response);

        assertThat(result).hasSize(1);
        NewsWebResponse webNews = result.get(0);
        assertThat(webNews.id()).isEqualTo(1L);
        assertThat(webNews.title()).isEqualTo("Dienstanweisung");
        assertThat(webNews.description()).isEqualTo("Beschreibung");
        assertThat(webNews.address()).isEqualTo("Raum 1.23");
        assertThat(webNews.timestamp()).isEqualTo(Instant.ofEpochSecond(epochSeconds));
        assertThat(webNews.survey()).isFalse();
        assertThat(webNews.answerable()).isTrue();
    }

    @Test
    void buildNewsListFiltersDeletedNews() {
        NewsResponse deleted = new NewsResponse(1L, null, "Gelöscht", null, null, null, false, false, true);
        NewsListResponse response = new NewsListResponse(true, new NewsListResponse.Data(Map.of("1", deleted), null));

        assertThat(service.buildNewsList(response)).isEmpty();
    }

    @Test
    void buildNewsListRespectsSortingOrder() {
        NewsResponse first = new NewsResponse(1L, null, "Erste", null, null, null, false, false, false);
        NewsResponse second = new NewsResponse(2L, null, "Zweite", null, null, null, false, false, false);
        NewsListResponse response = new NewsListResponse(true,
                new NewsListResponse.Data(Map.of("1", first, "2", second), List.of(2L, 1L)));

        List<NewsWebResponse> result = service.buildNewsList(response);

        assertThat(result).extracting(NewsWebResponse::title).containsExactly("Zweite", "Erste");
    }

    @Test
    void buildNewsListFallsBackToUnsortedWhenSortingIsMissing() {
        NewsResponse news = new NewsResponse(1L, null, "Ohne Sortierung", null, null, null, false, false, false);
        NewsListResponse response = new NewsListResponse(true, new NewsListResponse.Data(Map.of("1", news), List.of()));

        assertThat(service.buildNewsList(response)).hasSize(1);
    }

    @Test
    void buildNewsListReturnsNewestFourNewsSortedByDateDescending() {
        NewsResponse oldest = new NewsResponse(1L, null, "Älteste", null, null, 1000L, false, false, false);
        NewsResponse newest = new NewsResponse(2L, null, "Neueste", null, null, 5000L, false, false, false);
        NewsResponse middle = new NewsResponse(3L, null, "Mittlere", null, null, 3000L, false, false, false);
        NewsResponse extra1 = new NewsResponse(4L, null, "Extra1", null, null, 2000L, false, false, false);
        NewsResponse extra2 = new NewsResponse(5L, null, "Extra2", null, null, 500L, false, false, false);
        NewsListResponse response = new NewsListResponse(true,
                new NewsListResponse.Data(Map.of("1", oldest, "2", newest, "3", middle, "4", extra1, "5", extra2), null));

        List<NewsWebResponse> result = service.buildNewsList(response);

        assertThat(result).hasSize(4);
        assertThat(result).extracting(NewsWebResponse::title)
                .containsExactly("Neueste", "Mittlere", "Extra1", "Älteste");
    }

    @Test
    void buildNewsListSortsNewsWithoutDateLast() {
        NewsResponse withDate = new NewsResponse(1L, null, "Mit Datum", null, null, 1000L, false, false, false);
        NewsResponse withoutDate = new NewsResponse(2L, null, "Ohne Datum", null, null, null, false, false, false);
        NewsListResponse response = new NewsListResponse(true,
                new NewsListResponse.Data(Map.of("1", withDate, "2", withoutDate), null));

        List<NewsWebResponse> result = service.buildNewsList(response);

        assertThat(result).extracting(NewsWebResponse::title).containsExactly("Mit Datum", "Ohne Datum");
    }

}