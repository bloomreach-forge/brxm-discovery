package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryApiClient;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
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

import javax.jcr.Session;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HstDiscoveryServiceTest {

    @Mock DiscoveryApiClient client;
    @Mock DiscoveryConfigProvider configProvider;
    @Mock DiscoveryPixelService pixelService;
    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock ResolvedMount resolvedMount;
    @Mock Mount mount;
    @Mock DiscoveryChannelInfo channelInfo;
    @Mock jakarta.servlet.http.HttpServletRequest servletRequest;

    private DiscoveryConfig validConfig;
    private DiscoveryCredentials validCredentials;
    private SearchResult searchResult;
    private SearchResponse searchResponse;
    private HstDiscoveryService service;

    /** In-memory attribute store that simulates HstRequest setAttribute/getAttribute. */
    private final Map<String, Object> attrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        DiscoveryRuntimeContextFactory factory = new DiscoveryRuntimeContextFactory(configProvider);
        service = new HstDiscoveryService(client, factory, pixelService, null);

        validConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                10, "");
        validCredentials = validConfig.credentials();

        searchResult = new SearchResult(List.of(), 0L, 0, 10, Map.of());
        searchResponse = new SearchResponse(searchResult, SearchMetadata.empty());

        // Wire mount → request context → config
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.getResolvedMount()).thenReturn(resolvedMount);
        lenient().when(resolvedMount.getMount()).thenReturn(mount);
        lenient().when(configProvider.get(nullable(Session.class))).thenReturn(validConfig);

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
        lenient().when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(request.getHeader("User-Agent")).thenReturn(null);
        lenient().when(request.getHeader("Accept-Language")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        lenient().when(requestContext.getServletRequest()).thenReturn(servletRequest);
        lenient().when(servletRequest.getParameter(anyString())).thenReturn(null);
        lenient().when(servletRequest.getParameterMap()).thenReturn(Map.of());
    }

    // ── search ─────────────────────────────────────────────────────────────────

    @Test
    void search_delegatesToClientWithResolvedConfig() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
        verify(client).search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class));
    }

    @Test
    void search_withComponentFallbacks_passedThroughToQueryBuilder() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request, new SearchRequestOptions(24, "price asc", null, "default", List.of(), null, null));

        assertSame(searchResult, result.result());
        ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(client).search(captor.capture(), eq(validCredentials), any(ClientContext.class));
        // URL param absent → component fallback of 24 applied
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    @Test
    void search_zeroPageSize_delegatesToNoArg() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);
        service.search(request, SearchRequestOptions.defaults());

        // both use the same underlying query (cache hit on second), client called once
        verify(client, times(1)).search(any(), any(), any());
    }

    @Test
    void search_usesRequestCacheOnSecondCall() {
        when(client.search(any(SearchQuery.class), any(), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);
        service.search(request);

        verify(client, times(1)).search(any(), any(), any());
    }

    @Test
    void search_withNamedBand_storesCacheUnderBandKey() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse r1 = service.search(request, new SearchRequestOptions(12, null, null, "band-a", List.of(), null, null));
        SearchResponse r2 = service.search(request, new SearchRequestOptions(12, null, null, "band-b", List.of(), null, null));

        // Two different bands → two client calls (independent caches)
        verify(client, times(2)).search(any(), any(), any());
        assertSame(searchResult, r1.result());
        assertSame(searchResult, r2.result());
    }

    @Test
    void search_withNamedBand_cacheHitOnSameBand() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request, new SearchRequestOptions(12, null, null, "band-a", List.of(), null, null));
        service.search(request, new SearchRequestOptions(12, null, null, "band-a", List.of(), null, null));

        // Same band → cache hit on second call
        verify(client, times(1)).search(any(), any(), any());
    }

    // ── browse ─────────────────────────────────────────────────────────────────

    @Test
    void browse_callsClientCategoryMethod() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 10, Map.of());
        var catResponse = new SearchResponse(catResult, SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(catResponse);

        SearchResponse result = service.browse(request, "cat-123");

        assertSame(catResult, result.result());
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validCredentials), any(ClientContext.class));
        assertEquals("cat-123", captor.getValue().categoryId());
    }

    @Test
    void browse_withComponentFallbacks_passedThroughToQueryBuilder() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 24, Map.of());
        var catResponse = new SearchResponse(catResult, SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(catResponse);

        SearchResponse result = service.browse(request, "cat-123", new SearchRequestOptions(24, "price asc", null, "default", List.of(), null, null));

        assertSame(catResult, result.result());
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validCredentials), any(ClientContext.class));
        assertEquals("cat-123", captor.getValue().categoryId());
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    @Test
    void browse_withNamedBand_storesCacheUnderBandKey() {
        var catResponse = new SearchResponse(new SearchResult(List.of(), 5L, 0, 10, Map.of()), SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(catResponse);

        service.browse(request, "cat-1", new SearchRequestOptions(10, null, null, "band-a", List.of(), null, null));
        service.browse(request, "cat-1", new SearchRequestOptions(10, null, null, "band-b", List.of(), null, null));

        verify(client, times(2)).category(any(), any(), any());
    }

    @Test
    void browse_withNamedBand_cacheHitOnSameBand() {
        when(client.category(any(CategoryQuery.class), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 5L, 0, 10, Map.of()), SearchMetadata.empty()));

        service.browse(request, "cat-1", new SearchRequestOptions(10, null, null, "band-a", List.of(), null, null));
        service.browse(request, "cat-1", new SearchRequestOptions(10, null, null, "band-a", List.of(), null, null));

        verify(client, times(1)).category(any(), any(), any());
    }

    // ── recommend ──────────────────────────────────────────────────────────────

    @Test
    void recommend_withWidgetIdAndType_passesBothToClient() {
        when(client.recommend(any(RecQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, "w-123", "keyword", null, null, 8, null, null);

        ArgumentCaptor<RecQuery> captor = ArgumentCaptor.forClass(RecQuery.class);
        verify(client).recommend(captor.capture(), any(), any());
        assertEquals("w-123", captor.getValue().widgetId());
        assertEquals("keyword", captor.getValue().widgetType());
    }

    @Test
    void recommend_nullWidgetId_usesEmptyString() {
        when(client.recommend(any(RecQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, null, "keyword", null, null, 8, null, null);

        ArgumentCaptor<RecQuery> captor = ArgumentCaptor.forClass(RecQuery.class);
        verify(client).recommend(captor.capture(), any(), any());
        assertEquals("", captor.getValue().widgetId());
    }

    @Test
    void recommend_itemWidget_withoutContextPid_returnsEmpty() {
        RecommendationResult result = service.recommend(request, "w-item", "item", null, null, 8, null, null);

        assertTrue(result.products().isEmpty());
        verify(client, never()).recommend(any(), any(), any());
    }

    @Test
    void recommend_returnsRecommendationResult() {
        var products = List.of(new ProductSummary("p1", "Shoe", null, null, null, null, null));
        when(client.recommend(any(RecQuery.class), any(), any(ClientContext.class))).thenReturn(new RecommendationResult("rid-1", products));

        RecommendationResult result = service.recommend(request, "w-123", null, null, null, 8, null, null);

        assertNotNull(result);
        assertEquals("rid-1", result.widgetResultId());
        assertEquals(1, result.products().size());
    }

    @Test
    void recommend_usesRequestCacheKeyedByEffectiveWidgetId() {
        when(client.recommend(any(RecQuery.class), any(), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, "w-123", null, null, null, 8, null, null);
        service.recommend(request, "w-123", null, null, null, 8, null, null);

        verify(client, times(1)).recommend(any(), any(), any());
    }

    @Test
    void recommend_sameQueryDifferentLabels_reusesRequestCache() {
        when(client.recommend(any(RecQuery.class), any(), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, "w-123", null, null, null, 8, null, null, "band-a");
        service.recommend(request, "w-123", null, null, null, 8, null, null, "band-b");

        verify(client, times(1)).recommend(any(), any(), any());
    }

    @Test
    void recommend_differentContextProductIds_doNotShareCache() {
        when(client.recommend(any(RecQuery.class), any(), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, "w-123", null, "sku-1", null, 8, null, null, "band-a");
        service.recommend(request, "w-123", null, "sku-2", null, 8, null, null, "band-b");

        verify(client, times(2)).recommend(any(), any(), any());
    }

    @Test
    void fetchProduct_firesProductPageViewPixel() {
        var product = new ProductSummary("pid-42", "Shoe", null, null, null, null, null);
        when(client.fetchProduct(eq("pid-42"), anyString(), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(java.util.Optional.of(product));

        service.fetchProduct(request, "pid-42");

        verify(pixelService).fireProductPageViewEvent(eq("pid-42"), eq("Shoe"), any(), any(), anyString(), eq(validCredentials), any(), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void fetchProduct_notFound_doesNotFirePixel() {
        when(client.fetchProduct(eq("pid-99"), anyString(), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(java.util.Optional.empty());

        service.fetchProduct(request, "pid-99");

        verify(pixelService, never()).fireProductPageViewEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fetchProduct_usesRequestCacheOnSecondCall() {
        var product = new ProductSummary("pid-42", "Shoe", null, null, null, null, null);
        when(client.fetchProduct(eq("pid-42"), anyString(), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(java.util.Optional.of(product));

        Optional<ProductSummary> first = service.fetchProduct(request, "pid-42");
        Optional<ProductSummary> second = service.fetchProduct(request, "pid-42");

        assertTrue(first.isPresent());
        assertSame(first.get(), second.orElseThrow());
        verify(client, times(1)).fetchProduct(eq("pid-42"), anyString(), eq(validCredentials), any(ClientContext.class));
        verify(pixelService, times(1)).fireProductPageViewEvent(eq("pid-42"), eq("Shoe"), any(), any(), anyString(),
                eq(validCredentials), any(), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void fetchProduct_blankPid_returnsEmptyWithoutCallingClient() {
        assertTrue(service.fetchProduct(request, " ").isEmpty());

        verify(client, never()).fetchProduct(anyString(), anyString(), any(), any());
        verify(pixelService, never()).fireProductPageViewEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── configFor ──────────────────────────────────────────────────────────────

    @Test
    void configFor_callsProviderWithRequestSession() {
        when(client.search(any(), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(configProvider).get(nullable(Session.class));
    }

    // ── configFor credential validation ───────────────────────────────────────

    @Test
    void configFor_withValidConfig_returnsConfig() {
        // validConfig already set up in setUp(); just verify the service passes it through
        when(client.search(any(), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<DiscoveryCredentials> credentialsCaptor = ArgumentCaptor.forClass(DiscoveryCredentials.class);
        verify(client).search(any(), credentialsCaptor.capture(), any());
        assertEquals("acct", credentialsCaptor.getValue().accountId());
        assertEquals("domain", credentialsCaptor.getValue().domainKey());
    }

    @Test
    void configFor_blankAccountId_throwsConfigurationException() {
        DiscoveryConfig noCredentials = new DiscoveryConfig(
                null, null, null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION", 10, "");
        when(configProvider.get(nullable(Session.class))).thenReturn(noCredentials);

        assertThrows(ConfigurationException.class, () -> service.search(request));
    }

    // ── pixel region resolution ────────────────────────────────────────────────

    @Test
    void resolvePixelRegion_nullChannelInfo_defaultsToUS() {
        System.clearProperty("brxdis.pixel.region");
        assertEquals("US", HstDiscoveryService.resolvePixelRegion(null));
    }

    @Test
    void resolvePixelRegion_channelInfoEU_returnsEU() {
        System.clearProperty("brxdis.pixel.region");
        when(channelInfo.getPixelRegion()).thenReturn("EU");
        assertEquals("EU", HstDiscoveryService.resolvePixelRegion(channelInfo));
    }

    @Test
    void resolvePixelRegion_sysPropWinsOverChannelInfo() {
        System.setProperty("brxdis.pixel.region", "EU");
        try {
            // channelInfo.getPixelRegion() is never called when sys prop is set
            assertEquals("EU", HstDiscoveryService.resolvePixelRegion(channelInfo));
        } finally {
            System.clearProperty("brxdis.pixel.region");
        }
    }

    @Test
    void resolvePixelRegion_channelInfoLowercase_normalizedToUppercase() {
        System.clearProperty("brxdis.pixel.region");
        when(channelInfo.getPixelRegion()).thenReturn("eu");
        assertEquals("EU", HstDiscoveryService.resolvePixelRegion(channelInfo));
    }

    @Test
    void search_pixelRegionFromChannelInfo_passedInFlags() {
        System.clearProperty("brxdis.pixel.region");
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelTestData()).thenReturn(false);
        when(channelInfo.getDiscoveryPixelDebug()).thenReturn(false);
        when(channelInfo.getPixelRegion()).thenReturn("EU");
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<PixelFlags> flagsCaptor = ArgumentCaptor.forClass(PixelFlags.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), flagsCaptor.capture());
        assertEquals("EU", flagsCaptor.getValue().region());
    }

    // ── programmatic overload ──────────────────────────────────────────────────

    @Test
    void search_programmaticOverload_delegatesDirectly() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, Map.of(), null, null, null);
        when(client.search(query, validCredentials, ClientContext.EMPTY)).thenReturn(searchResponse);

        SearchResponse result = service.search(requestContext, query);

        assertSame(searchResult, result.result());
    }

    // ── pixel events ───────────────────────────────────────────────────────────

    @Test
    void search_firesPixelEventOnCacheMiss() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService).fireSearchEvent(any(SearchQuery.class), eq(searchResult), eq(validCredentials), any(), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void search_doesNotFirePixelEventOnCacheHit() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);   // cache miss → fires
        service.search(request);   // cache hit  → should not fire again

        verify(pixelService, times(1)).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), any());
    }

    @Test
    void search_nullPixelService_doesNotThrow() {
        HstDiscoveryService noPixel = new HstDiscoveryService(
                client, new DiscoveryRuntimeContextFactory(configProvider), null, null);
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        assertDoesNotThrow(() -> noPixel.search(request));
    }

    @Test
    void search_appliesEnrichmentOnCacheMiss() {
        SoREnrichmentProvider enricher = mock(SoREnrichmentProvider.class);
        HstDiscoveryService enrichedService = new HstDiscoveryService(
                client, new DiscoveryRuntimeContextFactory(configProvider), pixelService, enricher);
        List<ProductSummary> enrichedProducts = List.of(
                new ProductSummary("e1", "Enriched", null, null, null, null, null));
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);
        when(enricher.enrich(searchResult.products())).thenReturn(enrichedProducts);

        SearchResponse result = enrichedService.search(request);

        assertEquals(enrichedProducts, result.result().products());
    }

    @Test
    void search_nullEnrichmentProvider_returnsOriginalResult() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
    }

    @Test
    void search_pixelsDisabledByChannel_doesNotFirePixel() {
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(false);
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService, never()).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), any());
    }

    @Test
    void search_pixelsEnabledWithTestData_passesTestDataFlag() {
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelTestData()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelDebug()).thenReturn(false);
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<PixelFlags> flagsCaptor = ArgumentCaptor.forClass(PixelFlags.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), flagsCaptor.capture());
        PixelFlags flags = flagsCaptor.getValue();
        assertTrue(flags.enabled());
        assertTrue(flags.testData());
        assertFalse(flags.debug());
    }

    // ── autosuggest ─────────────────────────────────────────────────────────

    @Test
    void autosuggest_delegatesToClientWithResolvedConfig() {
        var expected = new AutosuggestResult("shoes", List.of("shoes"), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(expected);

        AutosuggestResult result = service.autosuggest(request, "shoes", 8);

        assertSame(expected, result);
        verify(client).autosuggest(any(AutosuggestQuery.class), eq(validCredentials), any(ClientContext.class));
    }

    @Test
    void autosuggest_passesQueryAndLimit() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(expected);

        service.autosuggest(request, "shi", 5);

        ArgumentCaptor<AutosuggestQuery> captor = ArgumentCaptor.forClass(AutosuggestQuery.class);
        verify(client).autosuggest(captor.capture(), any(), any());
        assertEquals("shi", captor.getValue().query());
        assertEquals(5, captor.getValue().limit());
    }

    @Test
    void autosuggest_noCaching_alwaysCallsClient() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(expected);

        service.autosuggest(request, "shi", 8);
        service.autosuggest(request, "shi", 8);

        verify(client, times(2)).autosuggest(any(), any(), any());
    }

    @Test
    void requestScopedRuntimeContext_reusesResolvedConfigAcrossOperations() {
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(new AutosuggestResult("shi", List.of(), List.of(), List.of()));

        service.search(request);
        service.autosuggest(request, "shi", 8);

        verify(configProvider, times(1)).get(nullable(Session.class));
    }

    // ── extractClientIp ────────────────────────────────────────────────────────

    @Test
    void search_extractsClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42, 10.0.0.1");
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<String> clientIpCaptor = ArgumentCaptor.forClass(String.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), clientIpCaptor.capture(), any(ClientContext.class), any(PixelFlags.class));
        assertEquals("203.0.113.42", clientIpCaptor.getValue(), "should use first token from X-Forwarded-For");
    }

    @Test
    void search_extractsClientIpFromRemoteAddr_whenNoXff() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<String> clientIpCaptor = ArgumentCaptor.forClass(String.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), clientIpCaptor.capture(), any(ClientContext.class), any(PixelFlags.class));
        assertEquals("192.168.1.10", clientIpCaptor.getValue(), "should fall back to getRemoteAddr()");
    }

    @Test
    void search_passesClientIpToPixel() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.1.2.3");
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService).fireSearchEvent(any(), any(), any(), eq("10.1.2.3"), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void search_extractsClientContextHeadersAndPassesToClient() {
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42, 10.0.0.1");
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<ClientContext> ctxCaptor = ArgumentCaptor.forClass(ClientContext.class);
        verify(client).search(any(SearchQuery.class), eq(validCredentials), ctxCaptor.capture());
        ClientContext ctx = ctxCaptor.getValue();
        assertEquals("Mozilla/5.0", ctx.userAgent());
        assertEquals("en-US,en;q=0.9", ctx.acceptLanguage());
        assertEquals("203.0.113.42, 10.0.0.1", ctx.xForwardedFor());
    }

    // ── channel credential overrides ────────────────────────────────────────

    @Test
    void channelAccountIdOverridesGlobalConfig() {
        // Create service backed by a factory with a custom envResolver so we can supply the channel apiKey
        DiscoveryRuntimeContextFactory factory = new DiscoveryRuntimeContextFactory(
                configProvider, name -> "CHAN_API_KEY".equals(name) ? "chan-api-key" : null);
        service = new HstDiscoveryService(client, factory, pixelService, null);

        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryAccountId()).thenReturn("chan-acct");
        when(channelInfo.getDiscoveryDomainKey()).thenReturn("chan-domain");
        when(channelInfo.getDiscoveryApiKeyEnvVar()).thenReturn("CHAN_API_KEY");
        when(channelInfo.getDiscoveryAuthKeyEnvVar()).thenReturn("");

        when(client.search(any(SearchQuery.class), any(DiscoveryCredentials.class), any(ClientContext.class)))
                .thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
        ArgumentCaptor<DiscoveryCredentials> credsCaptor = ArgumentCaptor.forClass(DiscoveryCredentials.class);
        verify(client).search(any(SearchQuery.class), credsCaptor.capture(), any(ClientContext.class));
        assertEquals("chan-acct", credsCaptor.getValue().accountId());
        assertEquals("chan-domain", credsCaptor.getValue().domainKey());
        assertEquals("chan-api-key", credsCaptor.getValue().apiKey());
        // global domain/key are NOT used — channel credentials replace entirely
        assertNull(credsCaptor.getValue().authKey());
    }

    @Test
    void noChannelInfo_usesGlobalConfig() {
        when(mount.getChannelInfo()).thenReturn(null);
        when(client.search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class)))
                .thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
        verify(client).search(any(SearchQuery.class), eq(validCredentials), any(ClientContext.class));
    }

    @Test
    void channelApiKeyEnvVar_configuredButMissing_throwsConfigException() {
        // Channel has opted-in by providing an env-var name, but the env var doesn't exist.
        // Full replacement means apiKey resolves to null → validation must throw ConfigurationException.
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryAccountId()).thenReturn("");
        when(channelInfo.getDiscoveryDomainKey()).thenReturn("");
        when(channelInfo.getDiscoveryApiKeyEnvVar()).thenReturn("BRXDIS_NONEXISTENT_ENV_VAR_XYZ");
        when(channelInfo.getDiscoveryAuthKeyEnvVar()).thenReturn("");

        assertThrows(ConfigurationException.class, () -> service.search(request));
    }
}
