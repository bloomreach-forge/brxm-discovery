package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryGridComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
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
    @Mock ResolvedSiteMapItem resolvedSiteMapItem;

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

    // ── No document → required ─────────────────────────────────────────────

    @Test
    void noDocument_noServiceCall_setsEmptyState() {
        buildNoDoc().doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("categoryId", "");
        verify(request).setModel("products", null);
    }

    /** RED: current code falls back to reading the URL param when no document is attached. */
    @Test
    void noDocument_withUrlParam_noServiceCall() {
        buildNoDocWithUrl("cat-123").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("products", null);
    }

    @Test
    void noDocument_editMode_noWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        buildNoDoc().doBeforeRender(request, response);

        // Channel Manager's own properties panel provides the "configure" affordance;
        // no redundant inline warning needed for the no-document case.
        verify(request, never()).setAttribute(eq("brxdis_warning"), any());
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
        buildNoDoc().doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("categoryId", "");
        verify(request).setModel("products", null);
    }

    @Test
    void dynamicDocument_noUrlParam_editMode_setsWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        // Document attached in Dynamic mode, no ?category= URL param → editor needs to know why component is empty
        buildDynamic(null, 12, "").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    // ── Dynamic mode — path segment takes precedence over query param ─────────

    @Test
    void document_dynamic_pathParamTakesPrecedenceOverQueryParam() {
        when(requestContext.getResolvedSiteMapItem()).thenReturn(resolvedSiteMapItem);
        when(resolvedSiteMapItem.getParameter("1")).thenReturn("path-cat");
        when(discoveryService.browse(eq(request), eq("path-cat"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        // URL param would give "query-cat" but path param "path-cat" must win
        new TestableCategoryGridComponent(discoveryService, "", "query-cat",
                12, "", true, true, true, Map.of())
                .doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("path-cat"), any(SearchRequestOptions.class));
        verify(discoveryService, never()).browse(eq(request), eq("query-cat"), any(SearchRequestOptions.class));
    }

    @Test
    void document_dynamic_fallsBackToQueryParam_whenPathParamAbsent() {
        // getResolvedSiteMapItem() returns null by default → path param absent
        when(discoveryService.browse(eq(request), eq("query-cat"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildDynamic("query-cat", 12, "").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("query-cat"), any(SearchRequestOptions.class));
    }

    // ── Dynamic mode (blank categoryId in doc) → URL param ────────────────

    @Test
    void document_dynamic_withUrlParam_callsBrowse() {
        when(discoveryService.browse(eq(request), eq("cat-dynamic"), any(SearchRequestOptions.class)))
                .thenReturn(new SearchResponse(singlePageResult, SearchMetadata.empty()));

        buildDynamic("cat-dynamic", 12, "").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("cat-dynamic"), any(SearchRequestOptions.class));
    }

    @Test
    void document_dynamic_noUrlParam_setsEmptyState() {
        buildDynamic(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("products", null);
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

    /** Pinned document with non-blank categoryId; no URL param. */
    private TestableCategoryGridComponent build(String categoryId, int pageSize, String sort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId, null,
                pageSize, sort, true, true, true, Map.of());
    }

    /** Pinned document; varies display flags. */
    private TestableCategoryGridComponent buildWith(String categoryId,
            boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId, null,
                12, "", showFacets, showPagination, showSort, Map.of());
    }

    /** Pinned document; varies display flags + servlet params. */
    private TestableCategoryGridComponent buildWithParams(String categoryId,
            Map<String, String[]> params, boolean showFacets, boolean showPagination, boolean showSort) {
        return new TestableCategoryGridComponent(discoveryService, categoryId, null,
                12, "", showFacets, showPagination, showSort, params);
    }

    /** No document attached; no URL param. */
    private TestableCategoryGridComponent buildNoDoc() {
        return new TestableCategoryGridComponent(discoveryService, null, null,
                12, "", true, true, true, Map.of());
    }

    /** No document attached; URL param provides a categoryId. */
    private TestableCategoryGridComponent buildNoDocWithUrl(String urlCategoryId) {
        return new TestableCategoryGridComponent(discoveryService, null, urlCategoryId,
                12, "", true, true, true, Map.of());
    }

    /** Document in dynamic mode (blank categoryId); URL param provides the categoryId. */
    private TestableCategoryGridComponent buildDynamic(String urlCategoryId, int pageSize, String sort) {
        return new TestableCategoryGridComponent(discoveryService, "", urlCategoryId,
                pageSize, sort, true, true, true, Map.of());
    }

    // ── Testable subclass ──────────────────────────────────────────────────

    private static class TestableCategoryGridComponent extends DiscoveryCategoryGridComponent {

        private final HstDiscoveryService service;
        /**
         * null  = no document attached<br>
         * ""    = document in Dynamic mode (blank categoryId, falls back to URL param)<br>
         * other = document in Pinned mode (explicit categoryId stored in document)
         */
        private final String docCategoryId;
        /** Value of the {@code ?category=} URL parameter. */
        private final String urlCategoryId;
        private final int pageSize;
        private final String sort;
        private final boolean showFacets;
        private final boolean showPagination;
        private final boolean showSort;
        private final Map<String, String[]> servletParams;

        TestableCategoryGridComponent(HstDiscoveryService service,
                String docCategoryId, String urlCategoryId,
                int pageSize, String sort,
                boolean showFacets, boolean showPagination, boolean showSort,
                Map<String, String[]> servletParams) {
            this.service = service;
            this.docCategoryId = docCategoryId;
            this.urlCategoryId = urlCategoryId;
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
                @Override public String getDocument()        { return docCategoryId != null ? "test-doc" : ""; }
                @Override public int getPageSize()           { return pageSize; }
                @Override public String getDefaultSort()     { return sort; }
                @Override public boolean isShowFacets()      { return showFacets; }
                @Override public boolean isShowPagination()  { return showPagination; }
                @Override public boolean isShowSort()        { return showSort; }
                @Override public String getCategoryUrlParam(){ return "category"; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> beanClass) {
            if (docCategoryId == null) return null;
            if (path == null || path.isBlank()) return null;
            return beanClass.cast(new DiscoveryCategoryBean() {
                @Override public String getCategoryId() { return docCategoryId; }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            if ("category".equals(name)) return urlCategoryId;
            return null;
        }

        @Override
        protected Map<String, String[]> getServletParameters(HstRequest request) {
            return servletParams;
        }
    }
}
