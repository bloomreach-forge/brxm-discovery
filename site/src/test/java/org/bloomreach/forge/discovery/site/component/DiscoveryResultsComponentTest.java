package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryResultsComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
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
class DiscoveryResultsComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult singlePageResult;
    private SearchResult multiPageResult;
    private SearchResult facetedResult;

    @BeforeEach
    void setUp() {
        var product = new ProductSummary("p1", "Shoe", "/shoe", null, BigDecimal.TEN, "USD", Map.of());
        singlePageResult = new SearchResult(List.of(product), 5L, 0, 12, Map.of());
        multiPageResult = new SearchResult(List.of(product), 30L, 0, 12, Map.of());
        facetedResult = new SearchResult(
                List.of(product), 10L, 0, 12,
                Map.of("color", new Facet("color", "text", List.of(
                        new FacetValue("red",  5L, null, null, null, null, null, null),
                        new FacetValue("blue", 3L, null, null, null, null, null, null))))
        );
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
    }

    // ── Search mode — blank / null query ───────────────────────────────────

    @Test
    void searchMode_nullQuery_noServiceCall_setsEmptyState() {
        buildSearch(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("query", "");
        verify(request).setModel("products", null);
    }

    @Test
    void searchMode_blankQuery_noServiceCall() {
        buildSearch("  ", 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
    }

    // ── Search mode — with query ────────────────────────────────────────────

    @Test
    void searchMode_withQuery_delegatesToServiceWithPageSizeAndSort() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildSearch("shoes", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).search(eq(request),
                argThat(o -> o.pageSize() == 24 && "price asc".equals(o.sort())));
    }

    @Test
    void searchMode_withQuery_setsProductsModel() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildSearch("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel("products", singlePageResult.products());
    }

    @Test
    void searchMode_withQuery_setsPaginationModel() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildSearch("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel(eq("pagination"), any(PaginationModel.class));
    }

    @Test
    void searchMode_withQuery_setsQueryModel_trimmed() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildSearch("  boots  ", 12, "").doBeforeRender(request, response);

        verify(request).setModel("query", "boots");
    }

    @Test
    void searchMode_withQuery_setsDataSourceMode() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildSearch("shoes", 12, "").doBeforeRender(request, response);

        verify(request).setModel("dataSourceMode", "search");
    }

    @Test
    void searchMode_withQuery_setsDidYouMean() {
        var meta = new SearchMetadata(Map.of(), List.of("boot"), null, null, null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildSearch("bott", 12, "").doBeforeRender(request, response);

        verify(request).setModel("didYouMean", List.of("boot"));
    }

    // ── Show facets toggle ─────────────────────────────────────────────────

    @Test
    void showFacets_true_setsFacetsAndFacetUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildSearchWith(null, true, false, false).doBeforeRender(request, response);

        verify(request).setModel(eq("facets"), any());
        verify(request).setModel(eq("facetUrls"), any());
        verify(request).setModel(eq("clearAllFiltersUrl"), any());
    }

    @Test
    void showFacets_false_doesNotSetFacetsOrFacetUrls() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildSearchWith(null, false, false, false).doBeforeRender(request, response);

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

        buildSearchWithParams("shoes", params, true).doBeforeRender(request, response);

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

        buildSearchWithParams("shoes", params, true).doBeforeRender(request, response);

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

        buildSearchWithParams("shoes", params, true).doBeforeRender(request, response);

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

        buildSearchWithParams("shoes", params, true).doBeforeRender(request, response);

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

        buildSearchWith(null, false, true, false).doBeforeRender(request, response);

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

        buildSearchWith(null, false, false, false).doBeforeRender(request, response);

        verify(request, never()).setModel(eq("pageUrls"), any());
    }

    @Test
    void pageUrl_forPage1_containsPageParam() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        buildSearchWithParams("shoes", params, false, true, false).doBeforeRender(request, response);

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

        buildSearchWith(null, false, true, false).doBeforeRender(request, response);

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

        buildSearchWith(null, false, false, true).doBeforeRender(request, response);

        verify(request).setModel(eq("sortUrl"), any(String.class));
    }

    @Test
    void sortUrl_doesNotContainSortParam() {
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("q", new String[]{"shoes"});
        params.put("sort", new String[]{"price+asc"});
        buildSearchWithParams("shoes", params, false, false, true).doBeforeRender(request, response);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).setModel(eq("sortUrl"), captor.capture());

        assertFalse(captor.getValue().contains("sort="), "Sort URL must strip sort param, got: " + captor.getValue());
        assertTrue(captor.getValue().contains("q=shoes"), "Sort URL must preserve query, got: " + captor.getValue());
    }

    // ── Category mode ──────────────────────────────────────────────────────

    @Test
    void categoryMode_withCategoryId_callsBrowse() {
        when(discoveryService.browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildCategory("cat-123", 12, "").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class));
    }

    @Test
    void categoryMode_noCategoryId_noServiceCall() {
        buildCategory(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("categoryId", "");
        verify(request).setModel("products", null);
    }

    @Test
    void categoryMode_noCategoryId_editMode_setsWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        buildCategory(null, 12, "").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void categoryMode_setsDataSourceMode() {
        when(discoveryService.browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildCategory("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel("dataSourceMode", "category");
    }

    @Test
    void categoryMode_setsDisplayNameFromMetadata() {
        var meta = new SearchMetadata(Map.of(), null, null, null, null, null, "Men's Shoes");
        when(discoveryService.browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildCategory("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel("displayName", "Men's Shoes");
    }

    // ── Auto-redirect ──────────────────────────────────────────────────────

    @Test
    void autoRedirect_enabled_withRedirectUrl_callsSendRedirect() throws Exception {
        var meta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildSearchWithAutoRedirect("shoes", true).doBeforeRender(request, response);

        verify(response).sendRedirect("https://example.com/sale");
    }

    @Test
    void autoRedirect_disabled_withRedirectUrl_setsModelOnly() throws Exception {
        var meta = new SearchMetadata(Map.of(), null, null, "https://example.com/sale", null);
        when(discoveryService.search(eq(request), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        buildSearchWithAutoRedirect("shoes", false).doBeforeRender(request, response);

        verify(response, never()).sendRedirect(any());
        verify(request).setModel("redirectUrl", "https://example.com/sale");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TestableResultsComponent buildSearch(String query, int pageSize, String sort) {
        return new TestableResultsComponent(discoveryService, "search", query, null,
                pageSize, sort, true, true, true, false, Map.of());
    }

    private TestableResultsComponent buildSearchWith(String query,
            boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableResultsComponent(discoveryService, "search",
                query != null ? query : "shoes", null,
                12, "", showFacets, showPagination, showSort, false, Map.of());
    }

    private TestableResultsComponent buildSearchWithParams(String query,
            Map<String, String[]> params, boolean showFacets) {
        return buildSearchWithParams(query, params, showFacets, false, false);
    }

    private TestableResultsComponent buildSearchWithParams(String query,
            Map<String, String[]> params, boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableResultsComponent(discoveryService, "search", query, null,
                12, "", showFacets, showPagination, showSort, false, params);
    }

    private TestableResultsComponent buildSearchWithAutoRedirect(String query, boolean autoRedirect) {
        return new TestableResultsComponent(discoveryService, "search", query, null,
                12, "", true, true, true, autoRedirect, Map.of());
    }

    private TestableResultsComponent buildCategory(String categoryId, int pageSize, String sort) {
        return new TestableResultsComponent(discoveryService, "category", null, categoryId,
                pageSize, sort, true, true, true, false, Map.of());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Testable subclass
    // ─────────────────────────────────────────────────────────────────────────

    private static class TestableResultsComponent extends DiscoveryResultsComponent {

        private final HstDiscoveryService service;
        private final String dataSource;
        private final String query;
        private final String categoryId;
        private final int pageSize;
        private final String sort;
        private final boolean showFacets;
        private final boolean showPagination;
        private final boolean showSort;
        private final boolean autoRedirect;
        private final Map<String, String[]> servletParams;

        TestableResultsComponent(HstDiscoveryService service, String dataSource,
                String query, String categoryId, int pageSize, String sort,
                boolean showFacets, boolean showPagination, boolean showSort,
                boolean autoRedirect, Map<String, String[]> servletParams) {
            this.service = service;
            this.dataSource = dataSource;
            this.query = query;
            this.categoryId = categoryId;
            this.pageSize = pageSize;
            this.sort = sort;
            this.showFacets = showFacets;
            this.showPagination = showPagination;
            this.showSort = showSort;
            this.autoRedirect = autoRedirect;
            this.servletParams = servletParams;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryResultsComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryResultsComponentInfo() {
                @Override public String getDataSource()        { return dataSource; }
                @Override public String getDocument()         { return ""; }
                @Override public int getPageSize()            { return pageSize; }
                @Override public String getDefaultSort()      { return sort; }
                @Override public String getCatalogName()      { return ""; }
                @Override public String getStatsFields()      { return ""; }
                @Override public String getSegment()          { return ""; }
                @Override public String getExclusionFilter()  { return ""; }
                @Override public boolean isShowFacets()       { return showFacets; }
                @Override public boolean isShowPagination()   { return showPagination; }
                @Override public boolean isShowSort()         { return showSort; }
                @Override public boolean isShowDidYouMean()   { return true; }
                @Override public boolean isAutoRedirect()     { return autoRedirect; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("q".equals(name)) return query;
            if ("category".equals(name)) return categoryId;
            return null;
        }

        @Override
        protected Map<String, String[]> getServletParameters(HstRequest request) {
            return servletParams;
        }
    }
}
