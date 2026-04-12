package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryGridComponentInfo;
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
class DiscoveryCategoryGridComponentTest {

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

    // ── Category ID resolution ─────────────────────────────────────────────

    @Test
    void withCategoryId_callsBrowseService() {
        when(discoveryService.browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("cat-123", 12, "").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("cat-123"), any(SearchRequestOptions.class));
    }

    @Test
    void withCategoryId_delegatesPageSizeAndSort() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("cat-123", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), any(),
                argThat(o -> o.pageSize() == 24 && "price asc".equals(o.sort())));
    }

    @Test
    void noCategoryId_noServiceCall_setsEmptyState() {
        build(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("categoryId", "");
        verify(request).setModel("products", null);
    }

    @Test
    void noCategoryId_editMode_setsWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        build(null, 12, "").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    // ── Result models ──────────────────────────────────────────────────────

    @Test
    void withCategoryId_setsProductsModel() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel("products", singlePageResult.products());
    }

    @Test
    void withCategoryId_setsPaginationModel() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel(eq("pagination"), any(PaginationModel.class));
    }

    @Test
    void withCategoryId_setsDataSourceModeToCategory() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        build("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel("dataSourceMode", "category");
    }

    @Test
    void withCategoryId_setsDisplayNameFromMetadata() {
        var meta = new SearchMetadata(Map.of(), null, null, null, null, null, "Men's Shoes");
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, meta));

        build("cat-123", 12, "").doBeforeRender(request, response);

        verify(request).setModel("displayName", "Men's Shoes");
    }

    // ── Show facets toggle ─────────────────────────────────────────────────

    @Test
    void showFacets_true_setsFacetsAndFacetUrls() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildWith("cat-123", true, false, false).doBeforeRender(request, response);

        verify(request).setModel(eq("facets"), any());
        verify(request).setModel(eq("facetUrls"), any());
        verify(request).setModel(eq("clearAllFiltersUrl"), any());
    }

    @Test
    void showFacets_false_doesNotSetFacetsOrFacetUrls() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(facetedResult, SearchMetadata.empty()));

        buildWith("cat-123", false, false, false).doBeforeRender(request, response);

        verify(request, never()).setModel(eq("facets"), any());
        verify(request, never()).setModel(eq("facetUrls"), any());
    }

    // ── Page URL building ──────────────────────────────────────────────────

    @Test
    void showPagination_true_buildsPageUrlsForEachPage() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        buildWith("cat-123", false, true, false).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("pageUrls"), captor.capture());

        // 30 results / 12 per page = 3 pages
        assertEquals(3, captor.getValue().size());
    }

    @Test
    void showPagination_false_doesNotSetPageUrls() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(multiPageResult, SearchMetadata.empty()));

        buildWith("cat-123", false, false, false).doBeforeRender(request, response);

        verify(request, never()).setModel(eq("pageUrls"), any());
    }

    // ── Sort URL ───────────────────────────────────────────────────────────

    @Test
    void showSort_true_setsSortUrl() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildWith("cat-123", false, false, true).doBeforeRender(request, response);

        verify(request).setModel(eq("sortUrl"), any(String.class));
    }

    @Test
    void sortUrl_doesNotContainSortParam() {
        when(discoveryService.browse(eq(request), any(), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        Map<String, String[]> params = new HashMap<>();
        params.put("category", new String[]{"cat-123"});
        params.put("sort", new String[]{"price+asc"});
        buildWithParams("cat-123", params, false, false, true).doBeforeRender(request, response);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(request).setModel(eq("sortUrl"), captor.capture());

        assertFalse(captor.getValue().contains("sort="),
                "Sort URL must strip sort param, got: " + captor.getValue());
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private TestableCategoryGridComponent build(String categoryId, int pageSize, String sort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId, pageSize, sort,
                true, true, true, Map.of());
    }

    private TestableCategoryGridComponent buildWith(String categoryId,
            boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId,
                12, "", showFacets, showPagination, showSort, Map.of());
    }

    private TestableCategoryGridComponent buildWithParams(String categoryId,
            Map<String, String[]> params, boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId,
                12, "", showFacets, showPagination, showSort, params);
    }

    // ── Testable subclass ──────────────────────────────────────────────────

    private static class TestableCategoryGridComponent extends DiscoveryCategoryGridComponent {

        private final HstDiscoveryService service;
        private final String categoryId;
        private final int pageSize;
        private final String sort;
        private final boolean showFacets;
        private final boolean showPagination;
        private final boolean showSort;
        private final Map<String, String[]> servletParams;

        TestableCategoryGridComponent(HstDiscoveryService service, String categoryId,
                int pageSize, String sort, boolean showFacets, boolean showPagination,
                boolean showSort, Map<String, String[]> servletParams) {
            this.service = service;
            this.categoryId = categoryId;
            this.pageSize = pageSize;
            this.sort = sort;
            this.showFacets = showFacets;
            this.showPagination = showPagination;
            this.showSort = showSort;
            this.servletParams = servletParams;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryCategoryGridComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryCategoryGridComponentInfo() {
                @Override public String getDocument()        { return ""; }
                @Override public int getPageSize()           { return pageSize; }
                @Override public String getDefaultSort()     { return sort; }
                @Override public String getStatsFields()     { return ""; }
                @Override public String getSegment()         { return ""; }
                @Override public String getExclusionFilter() { return ""; }
                @Override public boolean isShowFacets()      { return showFacets; }
                @Override public boolean isShowPagination()  { return showPagination; }
                @Override public boolean isShowSort()        { return showSort; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("category".equals(name)) return categoryId;
            return null;
        }

        @Override
        protected Map<String, String[]> getServletParameters(HstRequest request) {
            return servletParams;
        }
    }
}
