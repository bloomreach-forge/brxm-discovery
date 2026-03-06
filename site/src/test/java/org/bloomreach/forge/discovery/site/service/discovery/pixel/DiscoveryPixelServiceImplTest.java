package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPixelServiceImplTest {

    @Mock DiscoveryClient client;

    private DiscoveryPixelServiceImpl service;
    private DiscoveryConfig config;

    @BeforeEach
    void setUp() {
        // Synchronous executor so pixel fires inline — no async race in tests
        service = new DiscoveryPixelServiceImpl(client, Runnable::run);
        config = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
    }

    @Test
    void fireSearchEvent_delegatesToClientAndFires() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(query, result, config)).thenReturn("/api/v1/pixel/?type=SearchResponse");

        service.fireSearchEvent(query, result, config);

        verify(client).buildSearchPixelPath(query, result, config);
        verify(client).firePixelEvent("/api/v1/pixel/?type=SearchResponse", config);
    }

    @Test
    void fireCategoryEvent_delegatesToClientAndFires() {
        CategoryQuery query = new CategoryQuery("cat-1", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildCategoryPixelPath(query, result, config)).thenReturn("/api/v1/pixel/?type=CategoryView");

        service.fireCategoryEvent(query, result, config);

        verify(client).buildCategoryPixelPath(query, result, config);
        verify(client).firePixelEvent("/api/v1/pixel/?type=CategoryView", config);
    }

    @Test
    void fireWidgetEvent_delegatesToClientAndFires() {
        RecQuery query = new RecQuery("item", "w-1", null, null, 8, null, null, null, null, null);
        List<ProductSummary> products = List.of();
        when(client.buildWidgetPixelPath(query, products, config)).thenReturn("/api/v1/pixel/?type=Widget");

        service.fireWidgetEvent(query, products, config);

        verify(client).buildWidgetPixelPath(query, products, config);
        verify(client).firePixelEvent("/api/v1/pixel/?type=Widget", config);
    }

    @Test
    void fireSearchEvent_clientThrows_doesNotPropagate() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(any(), any(), any())).thenReturn("/api/v1/pixel/?type=SearchResponse");
        doThrow(new RuntimeException("broker down")).when(client).firePixelEvent(anyString(), any());

        assertDoesNotThrow(() -> service.fireSearchEvent(query, result, config));
    }
}
