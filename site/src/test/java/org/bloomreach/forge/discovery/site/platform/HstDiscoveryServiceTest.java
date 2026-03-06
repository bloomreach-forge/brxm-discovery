package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigResolver;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.sor.SoREnrichmentProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HstDiscoveryServiceTest {

    @Mock DiscoveryClient client;
    @Mock DiscoveryConfigProvider configProvider;
    @Mock DiscoveryPixelService pixelService;
    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock ResolvedMount resolvedMount;
    @Mock Mount mount;
    @Mock jakarta.servlet.http.HttpServletRequest servletRequest;

    private DiscoveryConfig validConfig;
    private SearchResult searchResult;
    private HstDiscoveryService service;

    /** In-memory attribute store that simulates HstRequest setAttribute/getAttribute. */
    private final Map<String, Object> attrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        service = new HstDiscoveryService(client, configProvider, pixelService, null);

        validConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");

        searchResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());

        // Wire mount → configPath → config
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.getResolvedMount()).thenReturn(resolvedMount);
        lenient().when(resolvedMount.getMount()).thenReturn(mount);
        lenient().when(mount.getParameter(DiscoveryConfigResolver.CONFIG_PATH_PARAM))
                .thenReturn("/hippo:config/discoveryConfig");
        lenient().when(configProvider.get("/hippo:config/discoveryConfig")).thenReturn(validConfig);

        // Simulate attribute storage on requestContext mock (DiscoveryRequestCache uses HstRequestContext)
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> { attrs.put((String) inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(requestContext).setAttribute(anyString(), any());

        // Default request params (empty search)
        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getScheme()).thenReturn("https");
        lenient().when(request.getServerName()).thenReturn("example.com");
        lenient().when(request.getServerPort()).thenReturn(443);
        lenient().when(request.getRequestURI()).thenReturn("/search");
        lenient().when(request.getQueryString()).thenReturn("q=shoes");
        lenient().when(request.getHeader("Referer")).thenReturn(null);
        lenient().when(requestContext.getServletRequest()).thenReturn(servletRequest);
        lenient().when(servletRequest.getParameter(anyString())).thenReturn(null);
        lenient().when(servletRequest.getParameterMap()).thenReturn(Map.of());
    }

    // ── search ─────────────────────────────────────────────────────────────────

    @Test
    void search_delegatesToClientWithResolvedConfig() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        SearchResult result = service.search(request);

        assertSame(searchResult, result);
        verify(client).search(any(SearchQuery.class), eq(validConfig));
    }

    @Test
    void search_withComponentFallbacks_passedThroughToQueryBuilder() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        SearchResult result = service.search(request, 24, "price asc");

        assertSame(searchResult, result);
        ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(client).search(captor.capture(), eq(validConfig));
        // URL param absent → component fallback of 24 applied
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    @Test
    void search_zeroPageSize_delegatesToNoArg() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        service.search(request);
        service.search(request, 0, null);

        // both use the same underlying query (cache hit on second), client called once
        verify(client, times(1)).search(any(), any());
    }

    @Test
    void search_usesRequestCacheOnSecondCall() {
        when(client.search(any(SearchQuery.class), any())).thenReturn(searchResult);

        service.search(request);
        service.search(request);

        verify(client, times(1)).search(any(), any());
    }

    // ── browse ─────────────────────────────────────────────────────────────────

    @Test
    void browse_callsClientCategoryMethod() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 10, Map.of());
        when(client.category(any(CategoryQuery.class), eq(validConfig))).thenReturn(catResult);

        SearchResult result = service.browse(request, "cat-123");

        assertSame(catResult, result);
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validConfig));
        assertEquals("cat-123", captor.getValue().categoryId());
    }

    @Test
    void browse_withComponentFallbacks_passedThroughToQueryBuilder() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 24, Map.of());
        when(client.category(any(CategoryQuery.class), eq(validConfig))).thenReturn(catResult);

        SearchResult result = service.browse(request, "cat-123", 24, "price asc");

        assertSame(catResult, result);
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validConfig));
        assertEquals("cat-123", captor.getValue().categoryId());
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    // ── recommend ──────────────────────────────────────────────────────────────

    @Test
    void recommend_withWidgetIdAndType_passesBothToClient() {
        when(client.recommend(any(RecQuery.class), eq(validConfig))).thenReturn(List.of());

        service.recommend(request, "w-123", "keyword", null, null, 8, null, null);

        ArgumentCaptor<RecQuery> captor = ArgumentCaptor.forClass(RecQuery.class);
        verify(client).recommend(captor.capture(), any());
        assertEquals("w-123", captor.getValue().widgetId());
        assertEquals("keyword", captor.getValue().widgetType());
    }

    @Test
    void recommend_nullWidgetId_usesEmptyString() {
        when(client.recommend(any(RecQuery.class), eq(validConfig))).thenReturn(List.of());

        service.recommend(request, null, "keyword", null, null, 8, null, null);

        ArgumentCaptor<RecQuery> captor = ArgumentCaptor.forClass(RecQuery.class);
        verify(client).recommend(captor.capture(), any());
        assertEquals("", captor.getValue().widgetId());
    }

    @Test
    void recommend_itemWidget_withoutContextPid_returnsEmpty() {
        List<ProductSummary> result = service.recommend(request, "w-item", "item", null, null, 8, null, null);

        assertTrue(result.isEmpty());
        verify(client, never()).recommend(any(), any());
    }

    @Test
    void recommend_usesRequestCacheKeyedByEffectiveWidgetId() {
        when(client.recommend(any(RecQuery.class), any())).thenReturn(List.of());

        service.recommend(request, "w-123", null, null, null, 8, null, null);
        service.recommend(request, "w-123", null, null, null, 8, null, null);

        verify(client, times(1)).recommend(any(), any());
    }

    // ── configFor ──────────────────────────────────────────────────────────────

    @Test
    void configFor_nullMountParam_passesNullToProvider() {
        when(mount.getParameter(DiscoveryConfigResolver.CONFIG_PATH_PARAM)).thenReturn(null);
        when(configProvider.get(null)).thenReturn(validConfig);
        when(client.search(any(), eq(validConfig))).thenReturn(searchResult);

        service.search(request);

        verify(configProvider).get(null);
    }

    // ── programmatic overload ──────────────────────────────────────────────────

    @Test
    void search_programmaticOverload_delegatesDirectly() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, Map.of(), null, null, null);
        when(configProvider.get("/hippo:config/discoveryConfig")).thenReturn(validConfig);
        when(client.search(query, validConfig)).thenReturn(searchResult);

        SearchResult result = service.search(requestContext, query);

        assertSame(searchResult, result);
    }

    // ── pixel events ───────────────────────────────────────────────────────────

    @Test
    void search_firesPixelEventOnCacheMiss() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        service.search(request);

        verify(pixelService).fireSearchEvent(any(SearchQuery.class), eq(searchResult), eq(validConfig));
    }

    @Test
    void search_doesNotFirePixelEventOnCacheHit() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        service.search(request);   // cache miss → fires
        service.search(request);   // cache hit  → should not fire again

        verify(pixelService, times(1)).fireSearchEvent(any(), any(), any());
    }

    @Test
    void search_nullPixelService_doesNotThrow() {
        HstDiscoveryService noPixel = new HstDiscoveryService(
                client, configProvider, null, null);
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        assertDoesNotThrow(() -> noPixel.search(request));
    }

    @Test
    void search_appliesEnrichmentOnCacheMiss() {
        SoREnrichmentProvider enricher = mock(SoREnrichmentProvider.class);
        HstDiscoveryService enrichedService = new HstDiscoveryService(
                client, configProvider, pixelService, enricher);
        List<ProductSummary> enrichedProducts = List.of(
                new ProductSummary("e1", "Enriched", null, null, null, null, null));
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);
        when(enricher.enrich(searchResult.products())).thenReturn(enrichedProducts);

        SearchResult result = enrichedService.search(request);

        assertEquals(enrichedProducts, result.products());
    }

    @Test
    void search_nullEnrichmentProvider_returnsOriginalResult() {
        when(client.search(any(SearchQuery.class), eq(validConfig))).thenReturn(searchResult);

        SearchResult result = service.search(request);

        assertSame(searchResult, result);
    }

    // ── autosuggest ─────────────────────────────────────────────────────────

    @Test
    void autosuggest_delegatesToClientWithResolvedConfig() {
        var expected = new AutosuggestResult("shoes", List.of("shoes"), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig))).thenReturn(expected);

        AutosuggestResult result = service.autosuggest(request, "shoes", 8);

        assertSame(expected, result);
        verify(client).autosuggest(any(AutosuggestQuery.class), eq(validConfig));
    }

    @Test
    void autosuggest_passesQueryAndLimit() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig))).thenReturn(expected);

        service.autosuggest(request, "shi", 5);

        ArgumentCaptor<AutosuggestQuery> captor = ArgumentCaptor.forClass(AutosuggestQuery.class);
        verify(client).autosuggest(captor.capture(), any());
        assertEquals("shi", captor.getValue().query());
        assertEquals(5, captor.getValue().limit());
    }

    @Test
    void autosuggest_withCatalogViews_passesThroughToQuery() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig))).thenReturn(expected);

        service.autosuggest(request, "shi", 8, "store:products_en");

        ArgumentCaptor<AutosuggestQuery> captor = ArgumentCaptor.forClass(AutosuggestQuery.class);
        verify(client).autosuggest(captor.capture(), any());
        assertEquals("store:products_en", captor.getValue().catalogViews());
    }

    @Test
    void autosuggest_noCaching_alwaysCallsClient() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig))).thenReturn(expected);

        service.autosuggest(request, "shi", 8);
        service.autosuggest(request, "shi", 8);

        verify(client, times(2)).autosuggest(any(), any());
    }
}
