package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchGridComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoverySearchGridComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult singlePageResult;
    private SearchResult multiPageResult;
    private SearchResult facetedResult;
    private SearchResult multiFacetResult;

    @BeforeEach
    void setUp() {
        var product = new ProductSummary("p1", "Shoe", "/shoe", null, BigDecimal.TEN, "USD", Map.of(), List.of());
        singlePageResult = new SearchResult(List.of(product), 5L, 0, 12, Map.of());
        multiPageResult = new SearchResult(List.of(product), 30L, 0, 12, Map.of());
        facetedResult = new SearchResult(
                List.of(product), 10L, 0, 12,
                Map.of("color", new Facet("color", "text", List.of(
                        new FacetValue("red",  5L, null, null, null, null, null, null),
                        new FacetValue("blue", 3L, null, null, null, null, null, null))))
        );
        multiFacetResult = new SearchResult(
                List.of(product), 10L, 0, 12,
                Map.of(
                    "color", new Facet("color", "text", List.of(new FacetValue("red", 5L, null, null, null, null, null, null))),
                    "size",  new Facet("size",  "text", List.of(new FacetValue("M",   4L, null, null, null, null, null, null)))
                )
        );
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
    }

    // ── Blank / null query ────────────────────────────────────────────────

    @Test
    void nullQuery_noServiceCall_setsEmptyState() {
        build(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
        verify(request).setModel("products", null);
    }

    @Test
    void blankQuery_noServiceCall() {
        build("  ", 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
    }

    // ── Query with results ─────────────────────────────────────────────────

    @Test
    void withQuery_delegatesToServiceWithPageSizeAndSort() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("shoes", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).search(eq(request),
                argThat(o -> o.pageSize() == 24 && "price asc".equals(o.sort())));
    }

    @Test
    void withQuery_setsProductsModel() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel("products", singlePageResult.products());
    }

    @Test
    void withQuery_setsPaginationModel() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel(eq("pagination"), any(PaginationModel.class));
    }

    @Test
    void withQuery_setsQueryModel_trimmed() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("  boots  ", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "boots");
    }

    @Test
    void withQuery_setsDataSourceModeToSearch() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel("dataSourceMode", "search");
    }

    @Test
    void withQuery_setsDidYouMean() {
        var meta = new SearchMetadata(Map.of(), List.of("boot"), null, null, null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        build("bott", 12, "").doBeforeRender(request, response);

        verify(request).setModel("didYouMean", List.of("boot"));
    }

    // ── Show facets toggle ─────────────────────────────────────────────────

    @Test
    void showFacets_true_setsFacetsAndFacetUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildWith(null, true, false, false).doBeforeRender(request, response);

        verify(request).setModel(eq("facets"), any());
        verify(request).setModel(eq("facetUrls"), any());
        verify(request).setModel(eq("clearAllFiltersUrl"), any());
    }

    @Test
    void showFacets_false_doesNotSetFacetsOrFacetUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildWith(null, false, false, false).doBeforeRender(request, response);

        verify(request, never()).setModel(eq("facets"), any());
        verify(request, never()).setModel(eq("facetUrls"), any());
    }

    // ── Facet URL building ─────────────────────────────────────────────────

    @Test
    void facetToggleUrl_activeValue_buildsRemoveUrl() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        params.put("filter.color", new String[]{"red"});

        buildWithParams("shoes", params, true).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Map<String, String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facetUrls"), captor.capture());

        String redUrl = captor.getValue().get("color").get("red");
        assertNotNull(redUrl, "URL for active 'red' must be present");
        assertFalse(redUrl.contains("filter.color=red"),
                "Active filter URL should remove it, but got: " + redUrl);
    }

    @Test
    void facetToggleUrl_inactiveValue_buildsAddUrl() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});

        buildWithParams("shoes", params, true).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Map<String, String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facetUrls"), captor.capture());

        String blueUrl = captor.getValue().get("color").get("blue");
        assertNotNull(blueUrl, "URL for inactive 'blue' must be present");
        assertTrue(blueUrl.contains("filter.color=blue"),
                "Inactive filter URL should add it, but got: " + blueUrl);
    }

    @Test
    void facetToggleUrl_pageParamIsRemovedWhenTogglingFilter() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        params.put("page", new String[]{"3"});

        buildWithParams("shoes", params, true).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Map<String, String>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facetUrls"), captor.capture());

        String redUrl = captor.getValue().get("color").get("red");
        assertFalse(redUrl.contains("page="), "Facet URL must reset page, but got: " + redUrl);
    }

    @Test
    void clearAllFiltersUrl_removesFilterParamsKeepsQuery() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        params.put("filter.color", new String[]{"red"});

        buildWithParams("shoes", params, true).doBeforeRender(request, response);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).setModel(eq("clearAllFiltersUrl"), captor.capture());

        String clearUrl = captor.getValue();
        assertFalse(clearUrl.contains("filter."), "Clear URL must not contain filter params, got: " + clearUrl);
        assertTrue(clearUrl.contains("q=shoes"), "Clear URL must preserve query param, got: " + clearUrl);
    }

    // ── Page URL building ──────────────────────────────────────────────────

    @Test
    void showPagination_true_buildsPageUrlsForEachPage() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        buildWith(null, false, true, false).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("pageUrls"), captor.capture());

        // 30 results / 12 per page = 3 pages (0-indexed: 0, 1, 2)
        assertEquals(3, captor.getValue().size());
    }

    @Test
    void showPagination_false_doesNotSetPageUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        buildWith(null, false, false, false).doBeforeRender(request, response);

        verify(request, never()).setModel(eq("pageUrls"), any());
    }

    @Test
    void pageUrl_forPage1_containsPageParam() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        buildWithParams("shoes", params, false, true, false).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("pageUrls"), captor.capture());

        String page1Url = captor.getValue().get("1");
        assertTrue(page1Url.contains("page=1"), "Page 1 URL must contain page=1, got: " + page1Url);
        assertTrue(page1Url.contains("q=shoes"), "Page 1 URL must preserve query param, got: " + page1Url);
    }

    @Test
    void pageUrl_forPage0_doesNotContainPageParam() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        buildWith(null, false, true, false).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("pageUrls"), captor.capture());

        String page0Url = captor.getValue().get("0");
        assertFalse(page0Url.contains("page="), "Page 0 URL must not contain page param, got: " + page0Url);
    }

    // ── Sort URL ───────────────────────────────────────────────────────────

    @Test
    void showSort_true_setsSortUrl() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWith(null, false, false, true).doBeforeRender(request, response);

        verify(request).setModel(eq("sortUrl"), any(String.class));
    }

    @Test
    void sortUrl_doesNotContainSortParam() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        params.put("sort", new String[]{"price+asc"});
        buildWithParams("shoes", params, false, false, true).doBeforeRender(request, response);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).setModel(eq("sortUrl"), captor.capture());

        assertFalse(captor.getValue().contains("sort="), "Sort URL must strip sort param, got: " + captor.getValue());
        assertTrue(captor.getValue().contains("q=shoes"), "Sort URL must preserve query, got: " + captor.getValue());
    }

    // ── Auto-redirect ──────────────────────────────────────────────────────

    @Test
    void autoRedirect_enabled_withRedirectUrl_callsSendRedirect() throws Exception {
        var meta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildWithAutoRedirect("shoes", true).doBeforeRender(request, response);

        verify(response).sendRedirect("https://example.com/sale");
    }

    @Test
    void autoRedirect_disabled_withRedirectUrl_setsModelOnly() throws Exception {
        var meta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildWithAutoRedirect("shoes", false).doBeforeRender(request, response);

        verify(response, never()).sendRedirect(any());
        verify(request).setModel("redirectUrl", "https://example.com/sale");
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private TestableSearchGridComponent build(String query, int pageSize, String sort) {
        return new TestableSearchGridComponent(discoveryService, query, pageSize, sort,
                true, true, true, false, Map.of(), "");
    }

    private TestableSearchGridComponent buildWith(String query,
            boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableSearchGridComponent(discoveryService,
                query != null ? query : "shoes",
                12, "", showFacets, showPagination, showSort, false, Map.of(), "");
    }

    private TestableSearchGridComponent buildWithParams(String query,
            Map<String, String[]> params, boolean showFacets) {
        return buildWithParams(query, params, showFacets, false, false);
    }

    private TestableSearchGridComponent buildWithParams(String query,
            Map<String, String[]> params, boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableSearchGridComponent(discoveryService, query,
                12, "", showFacets, showPagination, showSort, false, params, "");
    }

    private TestableSearchGridComponent buildWithAutoRedirect(String query, boolean autoRedirect) {
        return new TestableSearchGridComponent(discoveryService, query,
                12, "", true, true, true, autoRedirect, Map.of(), "");
    }

    private TestableSearchGridComponent buildWithFacetFields(String facetFields) {
        return new TestableSearchGridComponent(discoveryService, "shoes",
                12, "", true, false, false, false, Map.of(), facetFields);
    }

    private TestableSearchGridComponent buildWithVisualSearch(boolean enabled, String widgetId) {
        return buildWithVisualSearchAndImageId(enabled, widgetId, null);
    }

    private TestableSearchGridComponent buildWithVisualSearchAndImageId(boolean enabled, String widgetId, String imageId) {
        return new TestableSearchGridComponent(discoveryService, "shoes",
                12, "", true, true, true, false, Map.of(), "") {
            @Override
            protected DiscoveryChannelInfo getChannelInfo(HstRequest req) {
                if (!enabled) return null;
                return new DiscoveryChannelInfo() {
                    @Override public String getDiscoveryAccountId()            { return ""; }
                    @Override public String getDiscoveryDomainKey()            { return ""; }
                    @Override public String getDiscoveryApiKeyEnvVar()         { return ""; }
                    @Override public String getDiscoveryAuthKeyEnvVar()        { return ""; }
                    @Override public String getDiscoveryDefaultFieldList()     { return ""; }
                    @Override public String getDiscoveryCatalogName()          { return ""; }
                    @Override public boolean getDiscoveryPixelsEnabled()       { return true; }
                    @Override public String getDiscoveryPixelConsentCookie()   { return ""; }
                    @Override public boolean getDiscoveryPixelTestData()       { return false; }
                    @Override public boolean getDiscoveryPixelDebug()          { return false; }
                    @Override public String getPixelRegion()                   { return "US"; }
                    @Override public boolean getDiscoveryVisualSearchEnabled() { return true; }
                    @Override public String getDiscoveryVisualSearchWidgetId() { return widgetId; }
                    @Override public Map<String, Object> getProperties()       { return Map.of(); }
                };
            }

            @Override
            public String getPublicRequestParameter(HstRequest request, String name) {
                if ("imageId".equals(name)) return imageId;
                return super.getPublicRequestParameter(request, name);
            }
        };
    }

    // ── Visual search model keys ───────────────────────────────────────────

    @Test
    void visualSearch_disabled_setsEnabledFalseAndOmitsUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWithVisualSearch(false, "wid123").doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", false);
        verify(request, never()).setModel(eq("visualSearchUploadUrl"), any());
        verify(request, never()).setModel(eq("visualSearchSearchUrl"), any());
    }

    @Test
    void visualSearch_enabledWithWidgetId_setsUploadUrlAndWidgetId() {
        when(request.getContextPath()).thenReturn("");
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWithVisualSearch(true, "wid123").doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", true);
        verify(request).setModel("visualSearchUploadUrl", "/_brxdis-api/visual-search/wid123/upload");
        verify(request).setModel("visualSearchWidgetId", "wid123");
        verify(request, never()).setModel(eq("visualSearchSearchUrl"), any());
    }

    @Test
    void visualSearch_enabledWithWidgetId_prefixesContextPath() {
        when(request.getContextPath()).thenReturn("/site");
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWithVisualSearch(true, "wid123").doBeforeRender(request, response);

        verify(request).setModel("visualSearchUploadUrl", "/site/_brxdis-api/visual-search/wid123/upload");
        verify(request, never()).setModel(eq("visualSearchSearchUrl"), any());
    }

    @Test
    void visualSearch_imageId_setsProductsAndSkipsKeywordSearch() {
        var vsProducts = List.of(new ProductSummary("v1", "Sneaker", "/s", null, BigDecimal.TEN, "USD", Map.of(), List.of()));
        when(request.getContextPath()).thenReturn("");
        when(discoveryService.visualSearch(eq(request), eq("wid123"), eq("img-abc"), isNull(), eq(12)))
                .thenReturn(vsProducts);

        buildWithVisualSearchAndImageId(true, "wid123", "img-abc").doBeforeRender(request, response);

        verify(request).setModel("dataSourceMode", "visual-search");
        verify(request).setModel("products", vsProducts);
        verify(discoveryService, never()).search(any(HstRequest.class), any(SearchRequestOptions.class));
    }

    @Test
    void visualSearch_enabledButBlankWidgetId_setsEnabledTrueAndOmitsUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWithVisualSearch(true, "").doBeforeRender(request, response);

        verify(request).setModel("visualSearchEnabled", true);
        verify(request, never()).setModel(eq("visualSearchUploadUrl"), any());
        verify(request, never()).setModel(eq("visualSearchSearchUrl"), any());
    }

    // ── Facet scoping ──────────────────────────────────────────────────────

    @Test
    void facetFields_empty_showsAllFacets() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiFacetResult, SearchMetadata.empty()));

        buildWithFacetFields("").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Facet>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facets"), captor.capture());
        assertTrue(captor.getValue().containsKey("color"), "Expected 'color' facet");
        assertTrue(captor.getValue().containsKey("size"),  "Expected 'size' facet");
    }

    @Test
    void facetFields_subset_showsOnlyNamedFacets() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiFacetResult, SearchMetadata.empty()));

        buildWithFacetFields("color").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Facet>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facets"), captor.capture());
        assertTrue(captor.getValue().containsKey("color"),  "Expected 'color' facet");
        assertFalse(captor.getValue().containsKey("size"),  "'size' facet must be excluded");
    }

    @Test
    void facetFields_unknownField_showsEmptyFacets() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiFacetResult, SearchMetadata.empty()));

        buildWithFacetFields("brand").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Facet>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("facets"), captor.capture());
        assertTrue(captor.getValue().isEmpty(), "Expected empty facets map for unknown field");
    }

    // ── Testable subclass ──────────────────────────────────────────────────

    private static class TestableSearchGridComponent extends DiscoverySearchGridComponent {

        private final HstDiscoveryService service;
        private final String query;
        private final int pageSize;
        private final String sort;
        private final boolean showFacets;
        private final boolean showPagination;
        private final boolean showSort;
        private final boolean autoRedirect;
        private final Map<String, String[]> servletParams;
        private final String facetFields;

        TestableSearchGridComponent(HstDiscoveryService service, String query,
                int pageSize, String sort, boolean showFacets, boolean showPagination,
                boolean showSort, boolean autoRedirect, Map<String, String[]> servletParams,
                String facetFields) {
            this.service = service;
            this.query = query;
            this.pageSize = pageSize;
            this.sort = sort;
            this.showFacets = showFacets;
            this.showPagination = showPagination;
            this.showSort = showSort;
            this.autoRedirect = autoRedirect;
            this.servletParams = servletParams;
            this.facetFields = facetFields;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoverySearchGridComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoverySearchGridComponentInfo() {
                @Override public int getPageSize()           { return pageSize; }
                @Override public String getDefaultSort()     { return sort; }
                @Override public boolean isShowFacets()      { return showFacets; }
                @Override public boolean isShowPagination()  { return showPagination; }
                @Override public boolean isShowSort()        { return showSort; }
                @Override public boolean isShowDidYouMean()  { return true; }
                @Override public boolean isAutoRedirect()    { return autoRedirect; }
                @Override public String getCatalogName()     { return ""; }
                @Override public String getStatsFields()     { return ""; }
                @Override public String getSegment()         { return ""; }
                @Override public String getExclusionFilter()     { return ""; }
                @Override public String getFacetFields()         { return facetFields; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("q".equals(name)) return query;
            return null;
        }

        @Override
        protected Map<String, String[]> getServletParameters(HstRequest request) {
            return servletParams;
        }
    }
}
