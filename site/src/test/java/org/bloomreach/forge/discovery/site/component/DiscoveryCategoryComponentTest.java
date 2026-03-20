package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryCategoryComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstRequestContext requestContext;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult categoryResult;
    private SearchResponse categoryResponse;

    @BeforeEach
    void setUp() {
        ProductSummary product = new ProductSummary("p1", "Shoe", "/shoe", null, BigDecimal.TEN, "USD", Map.of());
        categoryResult = new SearchResult(List.of(product), 36L, 1, 12, Map.of());
        categoryResponse = new SearchResponse(categoryResult, SearchMetadata.empty());
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
    }

    private TestableCategoryComponent componentWith(String categoryId, int pageSize, String defaultSort) {
        return new TestableCategoryComponent(discoveryService, categoryId, pageSize, defaultSort);
    }

    // ── service delegation ──────────────────────────────────────────────────

    @Test
    void delegatesToServiceWithCategoryIdAndComponentParams() {
        when(discoveryService.browse(eq(request), eq("cat-123"),
                argThat(o -> o.pageSize() == 24 && "price asc".equals(o.sort()))))
                .thenReturn(categoryResponse);

        componentWith("cat-123", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("cat-123"),
                argThat(o -> o.pageSize() == 24 && "price asc".equals(o.sort())));
    }

    @Test
    void delegatesToServiceWithZeroPageSizeWhenNotSet() {
        when(discoveryService.browse(eq(request), eq("shoes"), argThat(o -> o.pageSize() == 0 && "".equals(o.sort()))))
                .thenReturn(categoryResponse);

        componentWith("shoes", 0, "").doBeforeRender(request, response);

        verify(discoveryService).browse(eq(request), eq("shoes"), argThat(o -> o.pageSize() == 0 && "".equals(o.sort())));
    }

    // ── model keys ──────────────────────────────────────────────────────────

    @Test
    void setsCategoryResultAndCategoryIdOnModel() {
        when(discoveryService.browse(eq(request), anyString(), any(SearchRequestOptions.class))).thenReturn(categoryResponse);

        componentWith("electronics", 12, "").doBeforeRender(request, response);

        verify(request).setModel("categoryResult", categoryResult);
        verify(request).setModel("categoryId", "electronics");
    }

    // ── null / blank categoryId guard ───────────────────────────────────────

    @Test
    void blankCategoryId_noServiceCall_setsEmptyResult() {
        componentWith("", 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("categoryId", "");
        verify(request).setModel(eq("categoryResult"),
                argThat(r -> r instanceof SearchResult sr && sr.total() == 0L && sr.products().isEmpty()));
    }

    @Test
    void nullCategoryId_noServiceCall() {
        componentWith(null, 12, "").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
    }

    // ── exception propagation ───────────────────────────────────────────────

    @Test
    void serviceThrows_exceptionPropagates() {
        when(discoveryService.browse(eq(request), eq("cat-1"), any(SearchRequestOptions.class)))
                .thenThrow(new RuntimeException("API down"));

        assertThrows(RuntimeException.class,
                () -> componentWith("cat-1", 12, "").doBeforeRender(request, response));
    }

    // ── testable subclass ───────────────────────────────────────────────────

    private static class TestableCategoryComponent extends DiscoveryCategoryComponent {

        private final HstDiscoveryService service;
        private final String categoryId;
        private final int pageSize;
        private final String defaultSort;

        TestableCategoryComponent(HstDiscoveryService service, String categoryId,
                                   int pageSize, String defaultSort) {
            this.service = service;
            this.categoryId = categoryId;
            this.pageSize = pageSize;
            this.defaultSort = defaultSort;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryCategoryComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryCategoryComponentInfo() {
                @Override public String getDocument() { return null; }
                @Override public int getPageSize() { return pageSize; }
                @Override public String getDefaultSort() { return defaultSort; }
                @Override public String getLabel() { return "default"; }
                @Override public String getStatsFields() { return ""; }
                @Override public String getSegment() { return ""; }
                @Override public String getExclusionFilter() { return ""; }
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return CAT_ID_PARAM.equals(name) ? categoryId : null;
        }
    }
}
