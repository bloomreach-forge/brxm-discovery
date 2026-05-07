package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductDetailComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import java.util.List;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import jakarta.servlet.http.HttpServletRequest;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryProductDetailComponentTest {

    @Mock HstRequest request;
    @Mock HstResponse response;
    @Mock HstDiscoveryService discoveryService;
    @Mock HstRequestContext requestContext;
    @Mock HttpServletRequest servletRequest;

    private final Map<String, Object> attrs = new HashMap<>();

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> {
            attrs.put((String) inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(requestContext).setAttribute(anyString(), any());
        lenient().when(requestContext.isChannelManagerPreviewRequest()).thenReturn(false);
    }

    /** No document attached; null = no URL param either. */
    private TestableProductDetailComponent noDoc() {
        return new TestableProductDetailComponent(discoveryService, null, null, "pid");
    }

    /** No document attached; URL param has this value. */
    private TestableProductDetailComponent noDoc(String urlPid) {
        return new TestableProductDetailComponent(discoveryService, null, urlPid, "pid");
    }

    /** Document in dynamic mode (blank productId); URL param provides the ID. */
    private TestableProductDetailComponent dynamic(String urlPid) {
        return new TestableProductDetailComponent(discoveryService, "", urlPid, "pid");
    }

    /** Document in dynamic mode, no URL param. */
    private TestableProductDetailComponent dynamic() {
        return new TestableProductDetailComponent(discoveryService, "", null, "pid");
    }

    /** Document in pinned mode with the given productId; no URL param. */
    private TestableProductDetailComponent pinned(String productId) {
        return new TestableProductDetailComponent(discoveryService, productId, null, "pid");
    }

    // ── No document → required ────────────────────────────────────────────────

    @Test
    void noDocument_noServiceCall_setsNullProduct() {
        noDoc().doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("product", null);
    }

    /** RED: current code falls back to reading the URL param even when no document is attached. */
    @Test
    void noDocument_withUrlParam_noServiceCall() {
        noDoc("p-1").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("product", null);
    }

    @Test
    void noDocument_editMode_noWarning() {
        when(requestContext.isChannelManagerPreviewRequest()).thenReturn(true);

        noDoc().doBeforeRender(request, response);

        // Channel Manager's own properties panel provides the "configure" affordance;
        // no redundant inline warning needed for the no-document case.
        verify(request, never()).setAttribute(eq("brxdis_warning"), any());
    }

    // ── Dynamic mode (blank productId) → URL param ────────────────────────────

    @Test
    void document_dynamic_withUrlParam_fetchesProduct() {
        ProductSummary product = new ProductSummary("p-1", "Test", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        dynamic("p-1").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("p-1"));
        verify(request).setModel("product", product);
    }

    @Test
    void document_dynamic_noUrlParam_setsNullProduct() {
        dynamic().doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("product", null);
    }

    // ── Dynamic mode - path segment takes precedence over query param ─────────

    @Test
    void document_dynamic_pathParamTakesPrecedenceOverQueryParam() {
        ProductSummary product = new ProductSummary("path-pid", "Test", null, null, null, null, Map.of(), List.of());
        when(requestContext.getServletRequest()).thenReturn(servletRequest);
        when(servletRequest.getPathInfo()).thenReturn("/product/blue-chair/pid/path-pid");
        when(discoveryService.fetchProduct(eq(request), eq("path-pid"))).thenReturn(Optional.of(product));

        // URL param is "query-pid" but path label wins
        new TestableProductDetailComponent(discoveryService, "", "query-pid", "pid")
                .doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("path-pid"));
        verify(discoveryService, never()).fetchProduct(eq(request), eq("query-pid"));
    }

    @Test
    void document_dynamic_fallsBackToQueryParam_whenPathParamAbsent() {
        ProductSummary product = new ProductSummary("query-pid", "Test", null, null, null, null, Map.of(), List.of());
        // getServletRequest() returns null by default → path label absent → falls back to query param
        when(discoveryService.fetchProduct(eq(request), eq("query-pid"))).thenReturn(Optional.of(product));

        dynamic("query-pid").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("query-pid"));
    }

    // ── Pinned mode (non-blank productId) → use pinned ID, ignore URL param ───

    @Test
    void document_pinned_usesPinnedId() {
        ProductSummary product = new ProductSummary("p99", "Test", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("p99"))).thenReturn(Optional.of(product));

        pinned("p99").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("p99"));
    }

    @Test
    void document_pinned_ignoresUrlParam() {
        ProductSummary product = new ProductSummary("doc-pid", "Test", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("doc-pid"))).thenReturn(Optional.of(product));

        new TestableProductDetailComponent(discoveryService, "doc-pid", "url-pid", "pid")
                .doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("doc-pid"));
        verify(discoveryService, never()).fetchProduct(eq(request), eq("url-pid"));
    }

    // ── Product found / not found ─────────────────────────────────────────────

    @Test
    void productFound_setsModel() {
        ProductSummary product = new ProductSummary("p-1", "Test", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        pinned("p-1").doBeforeRender(request, response);

        verify(request).setModel("product", product);
    }

    @Test
    void productNotFound_setsNullProduct() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        pinned("p-99").doBeforeRender(request, response);

        verify(request).setModel("product", null);
    }

    // ── PID exposed as model attribute ────────────────────────────────────────

    @Test
    void pid_exposedAsModelAttribute() {
        when(discoveryService.fetchProduct(eq(request), eq("bad-pid"))).thenReturn(Optional.empty());

        pinned("bad-pid").doBeforeRender(request, response);

        verify(request).setModel("pid", "bad-pid");
    }

    // ── Custom URL param name (dynamic mode) ──────────────────────────────────

    @Test
    void document_dynamic_customUrlParam_used() {
        ProductSummary product = new ProductSummary("some-sku", "Test", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("some-sku"))).thenReturn(Optional.of(product));

        new TestableProductDetailComponent(discoveryService, "", "some-sku", "sku")
                .doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("some-sku"));
    }

    // ── Band publication ──────────────────────────────────────────────────────

    @Test
    void marksBandPresent_whenProductFound() {
        ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        pinned("p-1").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.isProductDetailRendered(request));
    }

    @Test
    void marksBandPresent_whenProductNotFound() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        pinned("p-99").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.isProductDetailRendered(request));
    }

    @Test
    void putsToCacheWhenProductFound() {
        ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of(), List.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        pinned("p-1").doBeforeRender(request, response);

        Optional<ProductSummary> cached = DiscoveryRequestCache.getProductResult(request);
        assertTrue(cached.isPresent());
        assertSame(product, cached.get());
    }

    @Test
    void doesNotPutToCache_whenProductNotFound() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        pinned("p-99").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.getProductResult(request).isEmpty());
    }

    // ── Testable subclass ─────────────────────────────────────────────────────

    private static class TestableProductDetailComponent extends DiscoveryProductDetailComponent {

        private final HstDiscoveryService service;
        /**
         * null  = no document attached (component has no document configured)<br>
         * ""    = document in Dynamic mode (blank productId, falls back to URL param)<br>
         * other = document in Pinned mode (explicit productId stored in document)
         */
        private final String documentPid;
        private final String urlPid;
        private final String productUrlParam;

        TestableProductDetailComponent(HstDiscoveryService service,
                                       String documentPid,
                                       String urlPid,
                                       String productUrlParam) {
            this.service = service;
            this.documentPid = documentPid;
            this.urlPid = urlPid;
            this.productUrlParam = productUrlParam;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T> T lookupService(Class<T> type) {
            return (T) service;
        }

        @Override
        protected DiscoveryProductDetailComponentInfo getComponentParametersInfo(HstRequest request) {
            return new DiscoveryProductDetailComponentInfo() {
                @Override public String getDocument()        { return documentPid != null ? "test-doc" : ""; }
                @Override public String getProductUrlParam() { return productUrlParam; }
            };
        }

        @Override
        @SuppressWarnings("unchecked")
        protected <T extends HippoBean> T getHippoBeanForPath(HstRequest request, String path, Class<T> beanClass) {
            if (documentPid == null) return null;
            if (path == null || path.isBlank()) return null;
            return beanClass.cast(new DiscoveryProductDetailBean() {
                @Override public String getProductId() { return documentPid; }
            });
        }

        @Override
        public String getPublicRequestParameter(HstRequest request, String name) {
            return productUrlParam.equals(name) ? urlPid : null;
        }
    }
}
