package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigResolver;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
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
import java.util.function.Function;

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
    @Mock DiscoveryChannelInfo channelInfo;
    @Mock jakarta.servlet.http.HttpServletRequest servletRequest;

    private DiscoveryConfig validConfig;
    private SearchResult searchResult;
    private SearchResponse searchResponse;
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
        searchResponse = new SearchResponse(searchResult, SearchMetadata.empty());

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
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
        verify(client).search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class));
    }

    @Test
    void search_withComponentFallbacks_passedThroughToQueryBuilder() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request, 24, "price asc");

        assertSame(searchResult, result.result());
        ArgumentCaptor<SearchQuery> captor = ArgumentCaptor.forClass(SearchQuery.class);
        verify(client).search(captor.capture(), eq(validConfig), any(ClientContext.class));
        // URL param absent → component fallback of 24 applied
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    @Test
    void search_zeroPageSize_delegatesToNoArg() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);
        service.search(request, 0, null);

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
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse r1 = service.search(request, 12, null, null, "band-a");
        SearchResponse r2 = service.search(request, 12, null, null, "band-b");

        // Two different bands → two client calls (independent caches)
        verify(client, times(2)).search(any(), any(), any());
        assertSame(searchResult, r1.result());
        assertSame(searchResult, r2.result());
    }

    @Test
    void search_withNamedBand_cacheHitOnSameBand() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request, 12, null, null, "band-a");
        service.search(request, 12, null, null, "band-a");

        // Same band → cache hit on second call
        verify(client, times(1)).search(any(), any(), any());
    }

    // ── browse ─────────────────────────────────────────────────────────────────

    @Test
    void browse_callsClientCategoryMethod() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 10, Map.of());
        var catResponse = new SearchResponse(catResult, SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(catResponse);

        SearchResponse result = service.browse(request, "cat-123");

        assertSame(catResult, result.result());
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validConfig), any(ClientContext.class));
        assertEquals("cat-123", captor.getValue().categoryId());
    }

    @Test
    void browse_withComponentFallbacks_passedThroughToQueryBuilder() {
        SearchResult catResult = new SearchResult(List.of(), 5L, 0, 24, Map.of());
        var catResponse = new SearchResponse(catResult, SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(catResponse);

        SearchResponse result = service.browse(request, "cat-123", 24, "price asc");

        assertSame(catResult, result.result());
        ArgumentCaptor<CategoryQuery> captor = ArgumentCaptor.forClass(CategoryQuery.class);
        verify(client).category(captor.capture(), eq(validConfig), any(ClientContext.class));
        assertEquals("cat-123", captor.getValue().categoryId());
        assertEquals(24, captor.getValue().pageSize());
        assertEquals("price asc", captor.getValue().sort());
    }

    @Test
    void browse_withNamedBand_storesCacheUnderBandKey() {
        var catResponse = new SearchResponse(new SearchResult(List.of(), 5L, 0, 10, Map.of()), SearchMetadata.empty());
        when(client.category(any(CategoryQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(catResponse);

        service.browse(request, "cat-1", 10, null, "band-a");
        service.browse(request, "cat-1", 10, null, "band-b");

        verify(client, times(2)).category(any(), any(), any());
    }

    @Test
    void browse_withNamedBand_cacheHitOnSameBand() {
        when(client.category(any(CategoryQuery.class), eq(validConfig), any(ClientContext.class)))
                .thenReturn(new SearchResponse(new SearchResult(List.of(), 5L, 0, 10, Map.of()), SearchMetadata.empty()));

        service.browse(request, "cat-1", 10, null, "band-a");
        service.browse(request, "cat-1", 10, null, "band-a");

        verify(client, times(1)).category(any(), any(), any());
    }

    // ── recommend ──────────────────────────────────────────────────────────────

    @Test
    void recommend_withWidgetIdAndType_passesBothToClient() {
        when(client.recommend(any(RecQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

        service.recommend(request, "w-123", "keyword", null, null, 8, null, null);

        ArgumentCaptor<RecQuery> captor = ArgumentCaptor.forClass(RecQuery.class);
        verify(client).recommend(captor.capture(), any(), any());
        assertEquals("w-123", captor.getValue().widgetId());
        assertEquals("keyword", captor.getValue().widgetType());
    }

    @Test
    void recommend_nullWidgetId_usesEmptyString() {
        when(client.recommend(any(RecQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(RecommendationResult.of(List.of()));

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
    void fetchProduct_firesProductPageViewPixel() {
        var product = new ProductSummary("pid-42", "Shoe", null, null, null, null, null);
        when(client.fetchProduct(eq("pid-42"), anyString(), eq(validConfig), any(ClientContext.class)))
                .thenReturn(java.util.Optional.of(product));

        service.fetchProduct(request, "pid-42");

        verify(pixelService).fireProductPageViewEvent(eq("pid-42"), eq("Shoe"), any(), any(), anyString(), eq(validConfig), any(), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void fetchProduct_notFound_doesNotFirePixel() {
        when(client.fetchProduct(eq("pid-99"), anyString(), eq(validConfig), any(ClientContext.class)))
                .thenReturn(java.util.Optional.empty());

        service.fetchProduct(request, "pid-99");

        verify(pixelService, never()).fireProductPageViewEvent(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // ── configFor ──────────────────────────────────────────────────────────────

    @Test
    void configFor_nullMountParam_passesNullToProvider() {
        when(mount.getParameter(DiscoveryConfigResolver.CONFIG_PATH_PARAM)).thenReturn(null);
        when(configProvider.get(null)).thenReturn(validConfig);
        when(client.search(any(), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(configProvider).get(null);
    }

    // ── credential patching from channel info ─────────────────────────────────

    @Test
    void patchFromChannelInfo_nullChannelInfo_returnsOriginalConfig() {
        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(validConfig, null);
        assertSame(validConfig, result);
    }

    @Test
    void patchFromChannelInfo_blankAccountId_usesChannelInfo() {
        DiscoveryConfig blankAccount = new DiscoveryConfig(
                "", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getAccountId()).thenReturn("ci-account");
        // getDomainKey() not stubbed → returns null → domainKey falls back to config.domainKey() = "domain"

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(blankAccount, channelInfo);

        assertEquals("ci-account", result.accountId());
        assertEquals("domain", result.domainKey());
    }

    @Test
    void patchFromChannelInfo_blankDomainKey_usesChannelInfo() {
        DiscoveryConfig blankDomain = new DiscoveryConfig(
                "acct", "", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        // getAccountId() not stubbed → returns null → accountId falls back to config.accountId() = "acct"
        when(channelInfo.getDomainKey()).thenReturn("ci-domain");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(blankDomain, channelInfo);

        assertEquals("acct", result.accountId());
        assertEquals("ci-domain", result.domainKey());
    }

    @Test
    void patchFromChannelInfo_nonBlankAllCredentials_returnsOriginalConfig() {
        // Config with all credentials populated; channelInfo returns same values → no patch needed
        DiscoveryConfig fullConfig = new DiscoveryConfig(
                "acct", "domain", "key", "auth",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getAccountId()).thenReturn("acct");
        when(channelInfo.getDomainKey()).thenReturn("domain");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("");   // blank → no env lookup
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");  // blank → no env lookup
        Function<String, String> neverCalled = k -> { throw new AssertionError("env lookup called with: " + k); };

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(fullConfig, channelInfo, neverCalled);

        assertSame(fullConfig, result);
    }

    @Test
    void patchFromChannelInfo_blankApiKey_resolvesFromEnvVar() {
        DiscoveryConfig blankApiKey = new DiscoveryConfig(
                "acct", "domain", "", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("MY_KEY");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(
                blankApiKey, channelInfo, k -> "MY_KEY".equals(k) ? "secret" : null);

        assertEquals("secret", result.apiKey());
    }

    @Test
    void patchFromChannelInfo_blankApiKey_missingEnvVar_remainsBlank() {
        DiscoveryConfig blankApiKey = new DiscoveryConfig(
                "acct", "domain", "", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("MISSING_VAR");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(
                blankApiKey, channelInfo, k -> null);

        assertEquals("", result.apiKey());
    }

    @Test
    void patchFromChannelInfo_blankApiKey_blankEnvVarName_skipped() {
        DiscoveryConfig blankApiKey = new DiscoveryConfig(
                "acct", "domain", "", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");
        Function<String, String> neverCalled = k -> { throw new AssertionError("lookup called with: " + k); };

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(blankApiKey, channelInfo, neverCalled);

        assertEquals("", result.apiKey());
    }

    @Test
    void patchFromChannelInfo_blankApiKeyEnvVar_keepsConfigApiKey() {
        // validConfig has apiKey="key"; no per-channel env var → apiKey unchanged
        when(channelInfo.getAccountId()).thenReturn("acct");
        when(channelInfo.getDomainKey()).thenReturn("domain");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("");   // blank → no env lookup
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");  // blank → no env lookup
        Function<String, String> neverCalled = k -> { throw new AssertionError("env lookup called with: " + k); };

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(validConfig, channelInfo, neverCalled);

        assertEquals("key", result.apiKey());
    }

    @Test
    void patchFromChannelInfo_channelInfoAccountId_overridesNonBlankConfigAccountId() {
        // config has stale values from JCR; channelInfo has current channel identifiers → channelInfo wins
        DiscoveryConfig staleConfig = new DiscoveryConfig(
                "old-account", "old-domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getAccountId()).thenReturn("6413");
        when(channelInfo.getDomainKey()).thenReturn("pacifichome");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(staleConfig, channelInfo);

        assertEquals("6413", result.accountId());
        assertEquals("pacifichome", result.domainKey());
        assertEquals("key", result.apiKey()); // unchanged — no per-channel env var configured
    }

    @Test
    void patchFromChannelInfo_blankAuthKey_resolvesFromEnvVar() {
        DiscoveryConfig blankAuth = new DiscoveryConfig(
                "acct", "domain", "key", "",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("MY_AUTH_KEY");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(
                blankAuth, channelInfo, k -> "MY_AUTH_KEY".equals(k) ? "auth-secret" : null);

        assertEquals("auth-secret", result.authKey());
    }

    @Test
    void patchFromChannelInfo_nullAuthKey_resolvesFromEnvVar() {
        // validConfig has authKey=null (v1 API path)
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("MY_AUTH_KEY");

        DiscoveryConfig result = HstDiscoveryService.patchFromChannelInfo(
                validConfig, channelInfo, k -> "MY_AUTH_KEY".equals(k) ? "auth-secret" : null);

        assertEquals("auth-secret", result.authKey());
    }

    @Test
    void configFor_blankAccountId_patchedFromChannelInfo() {
        DiscoveryConfig blankAccount = new DiscoveryConfig(
                "", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(configProvider.get("/hippo:config/discoveryConfig")).thenReturn(blankAccount);
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getAccountId()).thenReturn("ci-account");
        // getDomainKey() not stubbed → returns null → domainKey falls back to config.domainKey() = "domain"
        when(client.search(any(), any(), any())).thenReturn(searchResponse);

        service.search(request);

        // Verify the config passed to client has the patched accountId
        ArgumentCaptor<DiscoveryConfig> configCaptor = ArgumentCaptor.forClass(DiscoveryConfig.class);
        verify(client).search(any(), configCaptor.capture(), any());
        assertEquals("ci-account", configCaptor.getValue().accountId());
    }

    @Test
    void configFor_blankCredentialsAfterPatching_throwsConfigurationException() {
        DiscoveryConfig noCredentials = new DiscoveryConfig(
                null, null, null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        when(configProvider.get("/hippo:config/discoveryConfig")).thenReturn(noCredentials);
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getAccountId()).thenReturn("");
        when(channelInfo.getDomainKey()).thenReturn("");
        when(channelInfo.getApiKeyEnvVar()).thenReturn("");
        when(channelInfo.getAuthKeyEnvVar()).thenReturn("");

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
        // getAccountId/getDomainKey not stubbed → return null → config values ("acct"/"domain") used unchanged
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<PixelFlags> flagsCaptor = ArgumentCaptor.forClass(PixelFlags.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), flagsCaptor.capture());
        assertEquals("EU", flagsCaptor.getValue().region());
    }

    // ── programmatic overload ──────────────────────────────────────────────────

    @Test
    void search_programmaticOverload_delegatesDirectly() {
        SearchQuery query = new SearchQuery("shoes", 0, 10, null, Map.of(), null, null, null);
        when(configProvider.get("/hippo:config/discoveryConfig")).thenReturn(validConfig);
        when(client.search(query, validConfig, ClientContext.EMPTY)).thenReturn(searchResponse);

        SearchResponse result = service.search(requestContext, query);

        assertSame(searchResult, result.result());
    }

    // ── pixel events ───────────────────────────────────────────────────────────

    @Test
    void search_firesPixelEventOnCacheMiss() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService).fireSearchEvent(any(SearchQuery.class), eq(searchResult), eq(validConfig), any(), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void search_doesNotFirePixelEventOnCacheHit() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);   // cache miss → fires
        service.search(request);   // cache hit  → should not fire again

        verify(pixelService, times(1)).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), any());
    }

    @Test
    void search_nullPixelService_doesNotThrow() {
        HstDiscoveryService noPixel = new HstDiscoveryService(
                client, configProvider, null, null);
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        assertDoesNotThrow(() -> noPixel.search(request));
    }

    @Test
    void search_appliesEnrichmentOnCacheMiss() {
        SoREnrichmentProvider enricher = mock(SoREnrichmentProvider.class);
        HstDiscoveryService enrichedService = new HstDiscoveryService(
                client, configProvider, pixelService, enricher);
        List<ProductSummary> enrichedProducts = List.of(
                new ProductSummary("e1", "Enriched", null, null, null, null, null));
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);
        when(enricher.enrich(searchResult.products())).thenReturn(enrichedProducts);

        SearchResponse result = enrichedService.search(request);

        assertEquals(enrichedProducts, result.result().products());
    }

    @Test
    void search_nullEnrichmentProvider_returnsOriginalResult() {
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        SearchResponse result = service.search(request);

        assertSame(searchResult, result.result());
    }

    @Test
    void search_pixelsDisabledByChannel_doesNotFirePixel() {
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(false);
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService, never()).fireSearchEvent(any(), any(), any(), any(), any(ClientContext.class), any());
    }

    @Test
    void search_pixelsEnabledWithTestData_passesTestDataFlag() {
        when(mount.getChannelInfo()).thenReturn(channelInfo);
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelTestData()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelDebug()).thenReturn(false);
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

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
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(expected);

        AutosuggestResult result = service.autosuggest(request, "shoes", 8);

        assertSame(expected, result);
        verify(client).autosuggest(any(AutosuggestQuery.class), eq(validConfig), any(ClientContext.class));
    }

    @Test
    void autosuggest_passesQueryAndLimit() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(expected);

        service.autosuggest(request, "shi", 5);

        ArgumentCaptor<AutosuggestQuery> captor = ArgumentCaptor.forClass(AutosuggestQuery.class);
        verify(client).autosuggest(captor.capture(), any(), any());
        assertEquals("shi", captor.getValue().query());
        assertEquals(5, captor.getValue().limit());
    }

    @Test
    void autosuggest_noCaching_alwaysCallsClient() {
        var expected = new AutosuggestResult("shi", List.of(), List.of(), List.of());
        when(client.autosuggest(any(AutosuggestQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(expected);

        service.autosuggest(request, "shi", 8);
        service.autosuggest(request, "shi", 8);

        verify(client, times(2)).autosuggest(any(), any(), any());
    }

    // ── extractClientIp ────────────────────────────────────────────────────────

    @Test
    void search_extractsClientIpFromXForwardedFor() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42, 10.0.0.1");
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<String> clientIpCaptor = ArgumentCaptor.forClass(String.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), clientIpCaptor.capture(), any(ClientContext.class), any(PixelFlags.class));
        assertEquals("203.0.113.42", clientIpCaptor.getValue(), "should use first token from X-Forwarded-For");
    }

    @Test
    void search_extractsClientIpFromRemoteAddr_whenNoXff() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("192.168.1.10");
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<String> clientIpCaptor = ArgumentCaptor.forClass(String.class);
        verify(pixelService).fireSearchEvent(any(), any(), any(), clientIpCaptor.capture(), any(ClientContext.class), any(PixelFlags.class));
        assertEquals("192.168.1.10", clientIpCaptor.getValue(), "should fall back to getRemoteAddr()");
    }

    @Test
    void search_passesClientIpToPixel() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.1.2.3");
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        verify(pixelService).fireSearchEvent(any(), any(), any(), eq("10.1.2.3"), any(ClientContext.class), any(PixelFlags.class));
    }

    @Test
    void search_extractsClientContextHeadersAndPassesToClient() {
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("Accept-Language")).thenReturn("en-US,en;q=0.9");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42, 10.0.0.1");
        when(client.search(any(SearchQuery.class), eq(validConfig), any(ClientContext.class))).thenReturn(searchResponse);

        service.search(request);

        ArgumentCaptor<ClientContext> ctxCaptor = ArgumentCaptor.forClass(ClientContext.class);
        verify(client).search(any(SearchQuery.class), eq(validConfig), ctxCaptor.capture());
        ClientContext ctx = ctxCaptor.getValue();
        assertEquals("Mozilla/5.0", ctx.userAgent());
        assertEquals("en-US,en;q=0.9", ctx.acceptLanguage());
        assertEquals("203.0.113.42, 10.0.0.1", ctx.xForwardedFor());
    }
}
