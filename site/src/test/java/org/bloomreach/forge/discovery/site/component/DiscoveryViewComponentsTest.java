package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryDataSourceComponentInfo;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryViewComponentsTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;

    private SearchResult searchResult;
    private SearchResult categoryResult;

    private static final String SEARCH_ATTR = "org.bloomreach.forge.discovery.requestCache.searchResult";
    private static final String CATEGORY_ATTR = "org.bloomreach.forge.discovery.requestCache.categoryResult";

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
    }

    // --- ProductGrid ---

    @Test
    void productGrid_searchDataSource_setsProductsModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResult);

        TestableProductGridComponent grid = new TestableProductGridComponent(null);
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
        when(requestContext.getAttribute(CATEGORY_ATTR)).thenReturn(categoryResult);

        TestableProductGridComponent grid = new TestableProductGridComponent("category");
        grid.doBeforeRender(request, response);

        verify(request).setModel("products", categoryResult.products());
    }

    @Test
    void productGrid_noData_setsEmptyList() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);

        TestableProductGridComponent grid = new TestableProductGridComponent(null);
        grid.doBeforeRender(request, response);

        verify(request).setModel(eq("products"), eq(List.of()));

        ArgumentCaptor<PaginationModel> captor = ArgumentCaptor.forClass(PaginationModel.class);
        verify(request).setModel(eq("pagination"), captor.capture());
        assertEquals(0L, captor.getValue().total());
        assertEquals(0, captor.getValue().page());
        assertEquals(0, captor.getValue().pageSize());
    }

    @Test
    void productGrid_noData_cmsPreview_setsWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new TestableProductGridComponent(null).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), contains("search data"));
    }

    @Test
    void productGrid_noData_liveMode_noWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        new TestableProductGridComponent(null).doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
    }

    // --- Facet ---

    @Test
    void facet_searchDataSource_setsFacetsModel() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(searchResult);

        TestableFacetComponent facet = new TestableFacetComponent(null);
        facet.doBeforeRender(request, response);

        verify(request).setModel("facets", searchResult.facets());
    }

    @Test
    void facet_noData_setsEmptyMap() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);

        TestableFacetComponent facet = new TestableFacetComponent(null);
        facet.doBeforeRender(request, response);

        verify(request).setModel(eq("facets"), eq(Map.of()));
    }

    @Test
    void facet_noData_cmsPreview_setsWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        new TestableFacetComponent(null).doBeforeRender(request, response);

        verify(request).setAttribute(eq("brxdis_warning"), contains("search data"));
    }

    @Test
    void facet_noData_liveMode_noWarning() {
        when(requestContext.getAttribute(SEARCH_ATTR)).thenReturn(null);
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);

        new TestableFacetComponent(null).doBeforeRender(request, response);

        verify(request, never()).setAttribute(eq("brxdis_warning"), anyString());
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

    // --- Testable subclasses that override getComponentParametersInfo ---

    private static class TestableProductGridComponent extends DiscoveryProductGridComponent {
        private final String dataSource;
        TestableProductGridComponent(String dataSource) { this.dataSource = dataSource; }
        @Override
        protected DiscoveryDataSourceComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryDataSourceComponentInfo() {
                @Override public String getDataSource() { return dataSource; }
            };
        }
    }

    private static class TestableFacetComponent extends DiscoveryFacetComponent {
        private final String dataSource;
        TestableFacetComponent(String dataSource) { this.dataSource = dataSource; }
        @Override
        protected DiscoveryDataSourceComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryDataSourceComponentInfo() {
                @Override public String getDataSource() { return dataSource; }
            };
        }
    }

}
