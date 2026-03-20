package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductHighlightComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryProductHighlightComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;

    @Test
    void noDocs_setsAllNullSlots() {
        new TestableProductHighlightComponent(discoveryService).doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("products"), captor.capture());
        assertEquals(4, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(p -> p == null));
        verifyNoInteractions(discoveryService);
    }

    @Test
    void oneDoc_fetchesProductInFirstSlot() {
        ProductSummary product = new ProductSummary("p1", "Product", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p1"))).thenReturn(Optional.of(product));

        new TestableProductHighlightComponent(discoveryService, "p1").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("products"), captor.capture());
        assertEquals(4, captor.getValue().size());
        assertSame(product, captor.getValue().get(0));
        assertTrue(captor.getValue().subList(1, 4).stream().allMatch(p -> p == null));
    }

    @Test
    void multipleDocsSamePid_fetchesEachInAlignedSlots() {
        ProductSummary p1 = new ProductSummary("p1", "P1", null, null, null, null, Map.of());
        ProductSummary p2 = new ProductSummary("p2", "P2", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p1"))).thenReturn(Optional.of(p1));
        when(discoveryService.fetchProduct(eq(request), eq("p2"))).thenReturn(Optional.of(p2));

        new TestableProductHighlightComponent(discoveryService, "p1", "p2").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("products"), captor.capture());
        assertEquals(4, captor.getValue().size());
        assertEquals(2, captor.getValue().stream().filter(p -> p != null).count());
        assertSame(p1, captor.getValue().get(0));
        assertSame(p2, captor.getValue().get(1));
    }

    @Test
    void productNotFound_slotIsNull() {
        when(discoveryService.fetchProduct(eq(request), eq("missing"))).thenReturn(Optional.empty());

        new TestableProductHighlightComponent(discoveryService, "missing").doBeforeRender(request, response);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductSummary>> captor = ArgumentCaptor.forClass(List.class);
        verify(request).setModel(eq("products"), captor.capture());
        assertEquals(4, captor.getValue().size());
        assertTrue(captor.getValue().stream().allMatch(p -> p == null));
    }

    // ── testable subclass ──────────────────────────────────────────────────────

    private static class TestableProductHighlightComponent extends DiscoveryProductHighlightComponent {

        private final HstDiscoveryService service;
        private final String[] pids;

        TestableProductHighlightComponent(HstDiscoveryService service, String... pids) {
            this.service = service;
            this.pids = pids;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryProductHighlightComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryProductHighlightComponentInfo() {
                @Override public String getDocument1() { return pids.length > 0 ? "doc1" : ""; }
                @Override public String getDocument2() { return pids.length > 1 ? "doc2" : ""; }
                @Override public String getDocument3() { return pids.length > 2 ? "doc3" : ""; }
                @Override public String getDocument4() { return pids.length > 3 ? "doc4" : ""; }
            };
        }

        private int callCount = 0;

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> beanClass) {
            if (callCount < pids.length) {
                final String pid = pids[callCount++];
                return beanClass.cast(new DiscoveryProductDetailBean() {
                    @Override public String getProductId() { return pid; }
                });
            }
            return null;
        }
    }
}
