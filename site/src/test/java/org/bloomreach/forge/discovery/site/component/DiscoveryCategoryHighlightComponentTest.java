package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryHighlightComponentInfo;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.platform.CategoryPreviewCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryCategoryHighlightComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;

    @Test
    void noPaths_setsEmptyList() {
        new TestableCategoryHighlightComponent(new DiscoveryCategoryBean[0]).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void onePath_resolvesOneBean() {
        DiscoveryCategoryBean bean = mock(DiscoveryCategoryBean.class);
        new TestableCategoryHighlightComponent(bean).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(1, captor.getValue().size());
        assertSame(bean, captor.getValue().get(0));
    }

    @Test
    void multiplePaths_collectsAllBeans() {
        DiscoveryCategoryBean b1 = mock(DiscoveryCategoryBean.class);
        DiscoveryCategoryBean b2 = mock(DiscoveryCategoryBean.class);
        new TestableCategoryHighlightComponent(b1, b2).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(2, captor.getValue().size());
    }

    @Test
    void nullBean_skipped() {
        DiscoveryCategoryBean b1 = mock(DiscoveryCategoryBean.class);
        TestableCategoryHighlightComponent comp = new TestableCategoryHighlightComponent(b1, null);
        comp.doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DiscoveryCategoryBean>> captor =
                ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("categories"), captor.capture());
        assertEquals(1, captor.getValue().size());
    }

    // ── previewProducts tests ─────────────────────────────────────────────────

    @Test
    void previewCount_zero_noBrowseCalled() {
        DiscoveryCategoryBean bean = mock(DiscoveryCategoryBean.class);
        when(bean.getCategoryId()).thenReturn("cat1");
        when(bean.getProductPreviewCount()).thenReturn(0);

        HstDiscoveryService svc = mock(HstDiscoveryService.class);
        new TestableCategoryHighlightComponent(svc, bean).doBeforeRender(request, response);

        verify(svc, never()).browse(any(), anyString(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<ProductSummary>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("previewProducts"), captor.capture());
        assertTrue(captor.getValue().isEmpty());
    }

    @Test
    void previewCount_two_browsesEachCategory() {
        DiscoveryCategoryBean b1 = mock(DiscoveryCategoryBean.class);
        when(b1.getCategoryId()).thenReturn("cat1");
        when(b1.getProductPreviewCount()).thenReturn(2);

        DiscoveryCategoryBean b2 = mock(DiscoveryCategoryBean.class);
        when(b2.getCategoryId()).thenReturn("cat2");
        when(b2.getProductPreviewCount()).thenReturn(2);

        ProductSummary p1 = new ProductSummary("pid1", "Shirt", null, null, null, null, Map.of());
        ProductSummary p2 = new ProductSummary("pid2", "Pants", null, null, null, null, Map.of());
        SearchResult result1 = new SearchResult(List.of(p1), 1, 0, 2, Map.of());
        SearchResult result2 = new SearchResult(List.of(p2), 1, 0, 2, Map.of());
        SearchResponse resp1 = new SearchResponse(result1, null);
        SearchResponse resp2 = new SearchResponse(result2, null);

        HstDiscoveryService svc = mock(HstDiscoveryService.class);
        when(svc.browse(eq(request), eq("cat1"), any(SearchRequestOptions.class))).thenReturn(resp1);
        when(svc.browse(eq(request), eq("cat2"), any(SearchRequestOptions.class))).thenReturn(resp2);

        new TestableCategoryHighlightComponent(svc, b1, b2).doBeforeRender(request, response);

        verify(svc, times(2)).browse(eq(request), anyString(), any(SearchRequestOptions.class));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<ProductSummary>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("previewProducts"), captor.capture());
        Map<String, List<ProductSummary>> map = captor.getValue();
        assertEquals(2, map.size());
        assertTrue(map.containsKey("cat1"));
        assertTrue(map.containsKey("cat2"));
        assertEquals(List.of(p1), map.get("cat1"));
        assertEquals(List.of(p2), map.get("cat2"));
    }

    // ── cache integration tests ────────────────────────────────────────────────

    @Test
    void cacheHit_noBrowseCalled() {
        DiscoveryCategoryBean bean = mock(DiscoveryCategoryBean.class);
        when(bean.getCategoryId()).thenReturn("cat1");
        when(bean.getProductPreviewCount()).thenReturn(2);

        ProductSummary cached = new ProductSummary("pid1", "Shirt", null, null, null, null, Map.of());
        CategoryPreviewCache cache = new CategoryPreviewCache();
        cache.put("cat1", 2, List.of(cached));

        HstDiscoveryService svc = mock(HstDiscoveryService.class);
        new TestableCategoryHighlightComponent(svc, cache, bean).doBeforeRender(request, response);

        verify(svc, never()).browse(any(), anyString(), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<ProductSummary>>> captor = ArgumentCaptor.forClass(Map.class);
        verify(request).setModel(eq("previewProducts"), captor.capture());
        assertEquals(List.of(cached), captor.getValue().get("cat1"));
    }

    @Test
    void cacheMiss_browsesAndPopulatesCache() {
        DiscoveryCategoryBean bean = mock(DiscoveryCategoryBean.class);
        when(bean.getCategoryId()).thenReturn("cat1");
        when(bean.getProductPreviewCount()).thenReturn(2);

        ProductSummary fetched = new ProductSummary("pid2", "Pants", null, null, null, null, Map.of());
        SearchResult result = new SearchResult(List.of(fetched), 1, 0, 2, Map.of());
        SearchResponse resp = new SearchResponse(result, null);

        HstDiscoveryService svc = mock(HstDiscoveryService.class);
        when(svc.browse(eq(request), eq("cat1"), any(SearchRequestOptions.class))).thenReturn(resp);

        CategoryPreviewCache cache = new CategoryPreviewCache();
        TestableCategoryHighlightComponent comp = new TestableCategoryHighlightComponent(svc, cache, bean);
        comp.doBeforeRender(request, response);

        verify(svc, times(1)).browse(eq(request), eq("cat1"), any(SearchRequestOptions.class));
        // entry is now in cache
        assertTrue(cache.get("cat1", 2).isPresent());
        assertEquals(List.of(fetched), cache.get("cat1", 2).get());
    }

    // ── testable subclass ──────────────────────────────────────────────────────

    private static class TestableCategoryHighlightComponent extends DiscoveryCategoryHighlightComponent {

        private final HstDiscoveryService discoveryService;
        private final CategoryPreviewCache previewCache;
        private final DiscoveryCategoryBean[] beans;
        private int callCount = 0;

        TestableCategoryHighlightComponent(DiscoveryCategoryBean... beans) {
            this(null, new CategoryPreviewCache(), beans);
        }

        TestableCategoryHighlightComponent(HstDiscoveryService discoveryService, DiscoveryCategoryBean... beans) {
            this(discoveryService, new CategoryPreviewCache(), beans);
        }

        TestableCategoryHighlightComponent(HstDiscoveryService discoveryService, CategoryPreviewCache cache,
                                            DiscoveryCategoryBean... beans) {
            this.discoveryService = discoveryService;
            this.previewCache = cache;
            this.beans = beans;
        }

        @Override
        protected HstDiscoveryService getDiscoveryService() {
            return discoveryService;
        }

        @Override
        protected CategoryPreviewCache getCategoryPreviewCache() {
            return previewCache;
        }

        @Override
        protected DiscoveryCategoryHighlightComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryCategoryHighlightComponentInfo() {
                @Override public String getDocument1() { return beans.length > 0 ? "doc1" : ""; }
                @Override public String getDocument2() { return beans.length > 1 ? "doc2" : ""; }
                @Override public String getDocument3() { return beans.length > 2 ? "doc3" : ""; }
                @Override public String getDocument4() { return beans.length > 3 ? "doc4" : ""; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> beanClass) {
            if (callCount < beans.length) {
                return beanClass.cast(beans[callCount++]);
            }
            return null;
        }
    }
}
