package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
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

    private static final PixelFlags ENABLED = new PixelFlags(true, false, false, "US");

    @Test
    void fireSearchEvent_delegatesToClientAndFires() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(query, result, config, null, ENABLED))
                .thenReturn("/api/v1/pixel/?type=SearchResponse");

        service.fireSearchEvent(query, result, config, null, ClientContext.EMPTY, ENABLED);

        verify(client).buildSearchPixelPath(query, result, config, null, ENABLED);
        verify(client).firePixelEvent("/api/v1/pixel/?type=SearchResponse", config, ClientContext.EMPTY, ENABLED);
    }

    @Test
    void fireCategoryEvent_delegatesToClientAndFires() {
        CategoryQuery query = new CategoryQuery("cat-1", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildCategoryPixelPath(query, result, config, null, ENABLED))
                .thenReturn("/api/v1/pixel/?type=CategoryView");

        service.fireCategoryEvent(query, result, config, null, ClientContext.EMPTY, ENABLED);

        verify(client).buildCategoryPixelPath(query, result, config, null, ENABLED);
        verify(client).firePixelEvent("/api/v1/pixel/?type=CategoryView", config, ClientContext.EMPTY, ENABLED);
    }

    @Test
    void fireWidgetEvent_withRecommendationResult_delegates() {
        RecQuery query = new RecQuery("item", "w-1", null, null, 8, null, null, null, null, null);
        RecommendationResult result = RecommendationResult.of(List.of());
        when(client.buildWidgetPixelPath(query, result, config, null, ENABLED))
                .thenReturn("/pix.gif?type=event&group=widget");

        service.fireWidgetEvent(query, result, config, null, ClientContext.EMPTY, ENABLED);

        verify(client).buildWidgetPixelPath(query, result, config, null, ENABLED);
        verify(client).firePixelEvent("/pix.gif?type=event&group=widget", config, ClientContext.EMPTY, ENABLED);
    }

    @Test
    void fireProductPageViewEvent_delegatesAndFires() {
        when(client.buildProductPageViewPixelPath("pid-42", "Shoe", "uid", "http://ref.com", "http://page.com",
                config, null, ENABLED))
                .thenReturn("/pix.gif?type=pageview&ptype=product&prod_id=pid-42");

        service.fireProductPageViewEvent("pid-42", "Shoe", "uid", "http://ref.com", "http://page.com", config, null, ClientContext.EMPTY, ENABLED);

        verify(client).buildProductPageViewPixelPath("pid-42", "Shoe", "uid", "http://ref.com", "http://page.com",
                config, null, ENABLED);
        verify(client).firePixelEvent("/pix.gif?type=pageview&ptype=product&prod_id=pid-42", config, ClientContext.EMPTY, ENABLED);
    }

    @Test
    void fireSearchEvent_passesClientIp() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(query, result, config, "10.0.0.1", ENABLED))
                .thenReturn("/pix.gif?type=pageview");

        service.fireSearchEvent(query, result, config, "10.0.0.1", ClientContext.EMPTY, ENABLED);

        verify(client).buildSearchPixelPath(query, result, config, "10.0.0.1", ENABLED);
    }

    @Test
    void fireSearchEvent_clientThrows_doesNotPropagate() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(any(), any(), any(), any(), any(PixelFlags.class)))
                .thenReturn("/api/v1/pixel/?type=SearchResponse");
        doThrow(new RuntimeException("broker down")).when(client).firePixelEvent(anyString(), any(), any(), any(PixelFlags.class));

        assertDoesNotThrow(() -> service.fireSearchEvent(query, result, config, null, ClientContext.EMPTY, ENABLED));
    }

    @Test
    void firePixelEvent_jsonParseError_doesNotPropagate() {
        // CRISP throws ResourceException("JSON processing error.") for image/gif responses on HTTP 200.
        // The service must not propagate this — the pixel was recorded successfully.
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(any(), any(), any(), any(), any(PixelFlags.class)))
                .thenReturn("/api/v1/pixel/?type=SearchResponse");
        doThrow(new ResourceException("JSON processing error.")).when(client).firePixelEvent(anyString(), any(), any(), any(PixelFlags.class));

        assertDoesNotThrow(() -> service.fireSearchEvent(query, result, config, null, ClientContext.EMPTY, ENABLED));
    }

    @Test
    void fireSearchEvent_disabled_doesNotFirePixel() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        service.fireSearchEvent(query, result, config, null, ClientContext.EMPTY, PixelFlags.DISABLED);

        verify(client, never()).buildSearchPixelPath(any(), any(), any(), any(), any(PixelFlags.class));
        verify(client, never()).firePixelEvent(anyString(), any(), any(), any(PixelFlags.class));
    }

    @Test
    void fireSearchEvent_enabled_firesPixel() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, null, null, null, null);
        SearchResult result = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        when(client.buildSearchPixelPath(any(), any(), any(), any(), any(PixelFlags.class)))
                .thenReturn("/pix.gif?type=pageview");

        service.fireSearchEvent(query, result, config, null, ClientContext.EMPTY, ENABLED);

        verify(client).firePixelEvent(anyString(), eq(config), any(), any(PixelFlags.class));
    }
}
