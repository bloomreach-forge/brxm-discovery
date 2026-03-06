package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryCategoryComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;

    private SearchResult categoryResult;

    @BeforeEach
    void setUp() {
        ProductSummary product = new ProductSummary("p1", "Shoe", "/shoe", null, BigDecimal.TEN, "USD", Map.of());
        categoryResult = new SearchResult(List.of(product), 36L, 1, 12, Map.of());
    }

    private TestableCategoryComponent componentWith(String categoryId, int pageSize, String defaultSort) {
        return new TestableCategoryComponent(discoveryService, categoryId, pageSize, defaultSort);
    }

    // ── service delegation ──────────────────────────────────────────────────

    @Test
    void delegatesToServiceWithCategoryIdAndComponentParams() {
        when(discoveryService.browse(request, "cat-123", 24, "price asc")).thenReturn(categoryResult);

        componentWith("cat-123", 24, "price asc").doBeforeRender(request, response);

        verify(discoveryService).browse(request, "cat-123", 24, "price asc");
    }

    @Test
    void delegatesToServiceWithZeroPageSizeWhenNotSet() {
        when(discoveryService.browse(eq(request), eq("shoes"), eq(0), eq(""))).thenReturn(categoryResult);

        componentWith("shoes", 0, "").doBeforeRender(request, response);

        verify(discoveryService).browse(request, "shoes", 0, "");
    }

    // ── model keys ──────────────────────────────────────────────────────────

    @Test
    void setsCategoryResultAndCategoryIdOnModel() {
        when(discoveryService.browse(eq(request), anyString(), anyInt(), any())).thenReturn(categoryResult);

        componentWith("electronics", 12, "").doBeforeRender(request, response);

        verify(request).setModel("categoryResult", categoryResult);
        verify(request).setAttribute("categoryResult", categoryResult);
        verify(request).setModel("categoryId", "electronics");
        verify(request).setAttribute("categoryId", "electronics");
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
            };
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return CAT_ID_PARAM.equals(name) ? categoryId : null;
        }
    }
}
