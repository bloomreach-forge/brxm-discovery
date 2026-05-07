package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.autosuggest.AutosuggestApiClient;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchPageView;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.RecommendationApiClient;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.SearchApiClient;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryClientTest {

    @Mock SearchApiClient searchClient;
    @Mock AutosuggestApiClient autosuggestClient;
    @Mock RecommendationApiClient recommendationClient;
    @Mock DiscoveryPixelTransport pixelTransport;

    private DiscoveryClientImpl client;
    private DiscoveryCredentials credentials;

    @BeforeEach
    void setUp() {
        client = new DiscoveryClientImpl(searchClient, autosuggestClient, recommendationClient, pixelTransport);
        credentials = new DiscoveryCredentials("acct123", "myDomain", "secret-key", null, "PRODUCTION");
    }

    // ── routing ──────────────────────────────────────────────────────────────

    @Test
    void search_delegatesToSearchClient() {
        var query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        var expected = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(searchClient.search(query, credentials, ClientContext.EMPTY)).thenReturn(expected);

        assertSame(expected, client.search(query, credentials, ClientContext.EMPTY));
        verify(searchClient).search(query, credentials, ClientContext.EMPTY);
    }

    @Test
    void category_delegatesToSearchClient() {
        var query = new CategoryQuery("cat-1", 0, 10, null, null, null, null, null);
        var expected = new SearchResponse(new SearchResult(List.of(), 0L, 0, 10, Map.of()), SearchMetadata.empty());
        when(searchClient.category(query, credentials, ClientContext.EMPTY)).thenReturn(expected);

        assertSame(expected, client.category(query, credentials, ClientContext.EMPTY));
        verify(searchClient).category(query, credentials, ClientContext.EMPTY);
    }

    @Test
    void fetchProduct_delegatesToSearchClient() {
        var product = new ProductSummary("p1", "Shoe", null, null, null, null, null, List.of());
        when(searchClient.fetchProduct("p1", null, "pid,title", credentials, ClientContext.EMPTY))
                .thenReturn(Optional.of(product));

        Optional<ProductSummary> result = client.fetchProduct("p1", null, "pid,title", credentials, ClientContext.EMPTY);

        assertTrue(result.isPresent());
        assertSame(product, result.get());
    }

    @Test
    void visualSearch_delegatesToSearchClient() {
        var query = new VisualSearchQuery("wid1", "img-abc", null, 10, null, null, null, null);
        var products = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null, List.of()));
        when(searchClient.visualSearch(query, credentials, ClientContext.EMPTY)).thenReturn(products);

        assertSame(products, client.visualSearch(query, credentials, ClientContext.EMPTY));
        verify(searchClient).visualSearch(query, credentials, ClientContext.EMPTY);
    }

    @Test
    void autosuggest_delegatesToAutosuggestClient() {
        var query = new AutosuggestQuery("shi", 8);
        var expected = new AutosuggestResult("shi", List.of("shirts"), List.of(), List.of());
        when(autosuggestClient.autosuggest(query, credentials, ClientContext.EMPTY)).thenReturn(expected);

        assertSame(expected, client.autosuggest(query, credentials, ClientContext.EMPTY));
        verify(autosuggestClient).autosuggest(query, credentials, ClientContext.EMPTY);
    }

    @Test
    void recommend_delegatesToRecommendationClient() {
        var query = new RecQuery("widget-1", "prod-42", "pdp", 6);
        var expected = RecommendationResult.of(List.of());
        when(recommendationClient.recommend(query, credentials, ClientContext.EMPTY)).thenReturn(expected);

        assertSame(expected, client.recommend(query, credentials, ClientContext.EMPTY));
        verify(recommendationClient).recommend(query, credentials, ClientContext.EMPTY);
    }

    @Test
    void buildPath_delegatesToPixelTransport() {
        var event = new SearchPageView(null, "shoes", List.of());
        var flags = mock(PixelFlags.class);
        when(pixelTransport.buildPath(event, credentials, "1.2.3.4", flags)).thenReturn("/pixel/path");

        assertEquals("/pixel/path", client.buildPath(event, credentials, "1.2.3.4", flags));
    }

    @Test
    void fire_delegatesToPixelTransport() {
        var flags = mock(PixelFlags.class);
        client.fire("/pixel/path", ClientContext.EMPTY, flags);
        verify(pixelTransport).fire("/pixel/path", ClientContext.EMPTY, flags);
    }

    // ── static utilities ─────────────────────────────────────────────────────

    @Test
    void toV2WidgetType_knownV2Types_passThrough() {
        for (String type : List.of("item", "keyword", "category", "personalized", "global", "visual")) {
            assertEquals(type, DiscoveryClientImpl.toV2WidgetType(type));
        }
    }

    @Test
    void toV2WidgetType_mlt_mapsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType("mlt"));
    }

    @Test
    void toV2WidgetType_unknownType_defaultsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType("legacy_unknown"));
    }

    @Test
    void toV2WidgetType_nullOrBlank_defaultsToItem() {
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType(null));
        assertEquals("item", DiscoveryClientImpl.toV2WidgetType(""));
    }

    @Test
    void requestId_extractsValueFromPath() {
        assertEquals("abc-uuid-123", DiscoveryClientImpl.requestId(
                "/api/v1/core/?account_id=123&request_id=abc-uuid-123&q=shoes"));
    }

    @Test
    void requestId_lastParam_extractsCorrectly() {
        assertEquals("my-uuid", DiscoveryClientImpl.requestId(
                "/api/v1/core/?account_id=123&request_id=my-uuid"));
    }

    @Test
    void requestId_missing_returnsNa() {
        assertEquals("n/a", DiscoveryClientImpl.requestId("/api/v1/core/?account_id=123"));
    }

    @Test
    void redactPath_replacesAuthKeyValue() {
        assertEquals("/api/v1/core?account_id=123&auth_key=***&q=shoes",
                DiscoveryClientImpl.redactPath("/api/v1/core?account_id=123&auth_key=super-secret&q=shoes"));
    }

    @Test
    void redactPath_noAuthKey_returnsPathUnchanged() {
        String path = "/api/v1/core?account_id=123&q=shoes";
        assertEquals(path, DiscoveryClientImpl.redactPath(path));
    }
}
