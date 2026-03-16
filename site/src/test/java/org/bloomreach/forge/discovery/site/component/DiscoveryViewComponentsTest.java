package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryDataSourceComponentInfo;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.container.HstContainerURL;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryViewComponentsTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstContainerURL baseUrl;

    private SearchResult searchResult;
    private SearchResult categoryResult;
    private SearchResponse searchResponse;
    private SearchResponse categoryResponse;

    private static final String SEARCH_ATTR         = "org.bloomreach.forge.discovery.requestCache.searchResult.default";
    private static final String CATEGORY_ATTR       = "org.bloomreach.forge.discovery.requestCache.categoryResult.default";
    private static final String SEARCH_BAND_MARKER  = "org.bloomreach.forge.discovery.requestCache.label.search.default";
    private static final String CAT_BAND_MARKER     = "org.bloomreach.forge.discovery.requestCache.label.category.default";

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);

        List<ProductSummary> products = List.of(
                new ProductSummary("p1", "Shoe", "http://shoe", "http://img", BigDecimal.TEN, "USD", Map.of())
        );
        Map<String, Facet> facets = Map.of(
                "brand", new Facet("brand", "text", List.of(new FacetValue("Nike", 10, null, null, null, null)))
        );
        searchResult = new SearchResult(products, 42L, 1, 12, facets);
        categoryResult = new SearchResult(products, 100L, 0, 24, facets);
        searchResponse = new SearchResponse(searchResult, SearchMetadata.empty());
        categoryResponse = new SearchResponse(categoryResult, SearchMetadata.empty());
    }

    // --- ProductGrid ---

    @Test
    void productGrid_searchDataSource_setsProductsModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResponse);

        TestableProductGridComponent grid = new TestableProductGridComponent();
        grid.doBeforeRender(request, response);

        verify(request).setModel("products", searchResult.products());
        verify(request).setAttribute("products", searchResult.products());

        ArgumentCaptor<PaginationModel> captor = ArgumentCaptor.forClass(PaginationModel.class);
        verify(request).setModel(eq("pagination"), captor.capture());
        PaginationModel model = captor.getValue();
        assertEquals(42L, model.total());
        assertEquals(1, model.page());
        assertEquals(12, model.pageSize());
        assertEquals(4, model.totalPages());
    }

    @Test
    void productGrid_categoryDataSource_readsCategoryResult() {
        // search probe returns null (default); lenient avoids PotentialStubbingProblem on SEARCH_ATTR call
        lenient().when(requestContext.getAttribute(CATEGORY_ATTR)).thenReturn(categoryResponse);

        TestableProductGridComponent grid = new TestableProductGridComponent();
        grid.doBeforeRender(request, response);

        verify(request).setModel("products", categoryResult.products());
    }

    @Test
    void productGrid_noData_setsEmptyList() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);

        TestableProductGridComponent grid = new TestableProductGridComponent();
        grid.doBeforeRender(request, response);

        verify(request).setModel(eq("products"), eq(List.of()));

        ArgumentCaptor<PaginationModel> captor = ArgumentCaptor.forClass(PaginationModel.class);
        verify(request).setModel(eq("pagination"), captor.capture());
        assertEquals(0L, captor.getValue().total());
        assertEquals(0, captor.getValue().page());
        assertEquals(0, captor.getValue().pageSize());
    }

    @Test
    void productGrid_noData_liveMode_noWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        new TestableProductGridComponent().doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void productGrid_bandPresentButNoResult_isBandConnected_noWarning() {
        // Marker set (data-source component ran) but no API result yet (e.g. no query typed) →
        // bandConnected=true, no warning even in CMS preview.
        // lenient: component also calls getAttribute for the result key (returns null); Mockito
        // strict mode would flag the band-marker stub as a "potential arg mismatch" otherwise.
        lenient().when(requestContext.getAttribute(SEARCH_BAND_MARKER)).thenReturn(Boolean.TRUE);

        new TestableProductGridComponent().doBeforeRender(request, response);

        verify(request).setModel("labelConnected", true);
        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void productGrid_bandAbsent_isNotBandConnected() {
        // No marker, no result → labelConnected=false
        new TestableProductGridComponent().doBeforeRender(request, response);

        verify(request).setModel("labelConnected", false);
    }

    // --- Facet ---

    @Test
    void facet_searchDataSource_setsFacetsModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResponse);

        TestableFacetComponent facet = new TestableFacetComponent();
        facet.doBeforeRender(request, response);

        verify(request).setModel("facets", searchResult.facets());
    }

    @Test
    void facet_noData_setsEmptyMap() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);

        TestableFacetComponent facet = new TestableFacetComponent();
        facet.doBeforeRender(request, response);

        verify(request).setModel(eq("facets"), eq(Map.of()));
    }

    @Test
    void facet_noData_liveMode_noWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        new TestableFacetComponent().doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void facet_bandPresentButNoResult_isBandConnected_noWarning() {
        lenient().when(requestContext.getAttribute(SEARCH_BAND_MARKER)).thenReturn(Boolean.TRUE);

        new TestableFacetComponent().doBeforeRender(request, response);

        verify(request).setModel("labelConnected", true);
        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void productGrid_editMode_bandAbsent_noWarning() {
        // Data component found in page tree → isBandConfiguredOnPage returns true
        // → no false "band not connected" warning.
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        TestableProductGridComponent component = new TestableProductGridComponent();
        component.setBandWiredOnPage(true);
        component.doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
        verify(request).setModel("labelConnected", true);
    }

    @Test
    void facet_editMode_bandAbsent_noWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        TestableFacetComponent component = new TestableFacetComponent();
        component.setBandWiredOnPage(true);
        component.doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
        verify(request).setModel("labelConnected", true);
    }

    @Test
    void productGrid_editMode_fullPageLoad_bandAbsent_setsWarning() {
        // Full-page CMS preview: isBandConfiguredOnPage override returns false (default) → warning IS shown.
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new TestableProductGridComponent().doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void facet_editMode_fullPageLoad_bandAbsent_setsWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new TestableFacetComponent().doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void productGrid_editMode_fullPageLoad_namedBandAbsent_setsWarningWithBandName() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        ArgumentCaptor<String> warningCaptor = ArgumentCaptor.forClass(String.class);
        new TestableProductGridComponent("search-bar").doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), warningCaptor.capture());
        assertTrue(warningCaptor.getValue().contains("search-bar"));
    }

    // --- PaginationModel math ---

    @Test
    void paginationModel_calculatesTotalPages() {
        assertEquals(4, new PaginationModel(42, 0, 12).totalPages());
        assertEquals(1, new PaginationModel(1, 0, 12).totalPages());
        assertEquals(0, new PaginationModel(0, 0, 12).totalPages());
        assertEquals(5, new PaginationModel(50, 0, 10).totalPages());
        assertEquals(0, new PaginationModel(10, 0, 0).totalPages());
    }

    // --- Band-aware cache reads ---

    @Test
    void productGrid_namedBand_readsFromNamedBandKey() {
        String namedBandAttr = "org.bloomreach.forge.discovery.requestCache.searchResult.my-band";
        when(requestContext.getAttribute(namedBandAttr)).thenReturn(searchResponse);

        new TestableProductGridComponent("my-band").doBeforeRender(request, response);

        verify(request).setModel("products", searchResult.products());
    }

    @Test
    void productGrid_namedBandMismatch_setsEmptyProducts() {
        // band "other" has no data; component returns empty products
        new TestableProductGridComponent("other").doBeforeRender(request, response);

        verify(request).setModel(eq("products"), eq(List.of()));
    }

    @Test
    void productGrid_setsLabelModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResponse);

        new TestableProductGridComponent("default").doBeforeRender(request, response);

        verify(request).setModel("label", "default");
        verify(request).setAttribute("label", "default");
    }

    @Test
    void facet_namedBand_readsFromNamedBandKey() {
        String namedBandAttr = "org.bloomreach.forge.discovery.requestCache.searchResult.promo-band";
        when(requestContext.getAttribute(namedBandAttr)).thenReturn(searchResponse);

        new TestableFacetComponent("promo-band").doBeforeRender(request, response);

        verify(request).setModel("facets", searchResult.facets());
    }

    @Test
    void facet_setsLabelModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResponse);

        new TestableFacetComponent("default").doBeforeRender(request, response);

        verify(request).setModel("label", "default");
        verify(request).setAttribute("label", "default");
    }

    // --- Backfill (consumer-before-producer / PPR) ---

    @Test
    void productGrid_cacheMiss_backfillsFromProducer_showsProducts() {
        TestableProductGridComponent grid = new TestableProductGridComponent();
        grid.setBackfillResponse(searchResponse);
        grid.doBeforeRender(request, response);

        verify(request).setModel("products", searchResult.products());
        verify(request).setModel("labelConnected", true);
        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void facet_cacheMiss_backfillsFromProducer_showsFacets() {
        TestableFacetComponent facet = new TestableFacetComponent();
        facet.setBackfillResponse(searchResponse);
        facet.doBeforeRender(request, response);

        verify(request).setModel("facets", searchResult.facets());
        verify(request).setModel("labelConnected", true);
        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    @Test
    void productGrid_producerAlreadyRan_noBackfill() {
        // Producer ran first → cache hit. Backfill should NOT be invoked.
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResponse);
        lenient().when(requestContext.getAttribute(SEARCH_BAND_MARKER)).thenReturn(Boolean.TRUE);

        TestableProductGridComponent grid = new TestableProductGridComponent();
        grid.setBackfillResponse(null); // would fail if called
        grid.doBeforeRender(request, response);

        verify(request).setModel("products", searchResult.products());
        verify(request).setModel("labelConnected", true);
    }

    // --- Testable subclasses that override getComponentParametersInfo ---

    private static class TestableProductGridComponent extends DiscoveryProductGridComponent {
        private final String bandName;
        private boolean bandWiredOnPage = false;
        private SearchResponse backfillResponse = null;

        TestableProductGridComponent() { this("default"); }
        TestableProductGridComponent(String bandName) {
            this.bandName = bandName;
        }

        void setBandWiredOnPage(boolean wired) { this.bandWiredOnPage = wired; }
        void setBackfillResponse(SearchResponse r) { this.backfillResponse = r; }

        @Override
        protected boolean isBandConfiguredOnPage(HstRequest req, String label, Class<?> dataComponentClass) {
            return bandWiredOnPage;
        }

        @Override
        protected Optional<SearchResponse> backfillSearchResponse(HstRequest req, String label) {
            return Optional.ofNullable(backfillResponse);
        }

        @Override
        protected DiscoveryDataSourceComponentInfo getComponentParametersInfo(HstRequest request) {
            return () -> bandName;
        }
    }

    private static class TestableFacetComponent extends DiscoveryFacetComponent {
        private final String bandName;
        private boolean bandWiredOnPage = false;
        private SearchResponse backfillResponse = null;

        TestableFacetComponent() { this("default"); }
        TestableFacetComponent(String bandName) {
            this.bandName = bandName;
        }

        void setBandWiredOnPage(boolean wired) { this.bandWiredOnPage = wired; }
        void setBackfillResponse(SearchResponse r) { this.backfillResponse = r; }

        @Override
        protected boolean isBandConfiguredOnPage(HstRequest req, String label, Class<?> dataComponentClass) {
            return bandWiredOnPage;
        }

        @Override
        protected Optional<SearchResponse> backfillSearchResponse(HstRequest req, String label) {
            return Optional.ofNullable(backfillResponse);
        }

        @Override
        protected DiscoveryDataSourceComponentInfo getComponentParametersInfo(HstRequest request) {
            return () -> bandName;
        }
    }

}
