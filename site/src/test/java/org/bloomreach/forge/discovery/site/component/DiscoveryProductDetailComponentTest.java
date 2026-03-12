package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductDetailComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
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

    /** No document, no bean resolution. */
    private TestableProductDetailComponent component(String urlPid) {
        return new TestableProductDetailComponent(discoveryService, urlPid, null, "brxdis:pid", null, "pid");
    }

    /** URL param + page bean resolution, no document. */
    private TestableProductDetailComponent component(String urlPid, String beanPid, String pidProperty) {
        return new TestableProductDetailComponent(discoveryService, urlPid, beanPid, pidProperty, null, "pid");
    }

    /** Full chain: URL param + page bean + document bean. */
    private TestableProductDetailComponent component(String urlPid, String beanPid,
                                                     String pidProperty, String documentPid) {
        return new TestableProductDetailComponent(discoveryService, urlPid, beanPid, pidProperty, documentPid, "pid");
    }

    // ── blank pid → early exit, service never called ──────────────────────────

    @Test
    void noProduct_pid_blank() {
        component("").doBeforeRender(request, response);

        verifyNoInteractions(discoveryService);
        verify(request).setModel("product", null);
        verify(request).setAttribute("product", null);
    }

    @Test
    void label_setEvenWhenPidBlank() {
        component("").doBeforeRender(request, response);

        verify(request).setModel("label", "default");
        verify(request).setAttribute("label", "default");
    }

    // ── product found → model set ─────────────────────────────────────────────

    @Test
    void productFound_setsModel() {
        ProductSummary product = new ProductSummary("p-1", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        component("p-1").doBeforeRender(request, response);

        verify(request).setModel("product", product);
        verify(request).setAttribute("product", product);
    }

    // ── product not found → null model ───────────────────────────────────────

    @Test
    void productNotFound_setsNullProduct() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        component("p-99").doBeforeRender(request, response);

        verify(request).setModel("product", null);
        verify(request).setAttribute("product", null);
    }

    // ── Stage 2: document bean provides PID when URL param absent ────────────

    @Test
    void pid_resolvedFromDocumentBean_whenNoUrlParam() {
        ProductSummary product = new ProductSummary("p99", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p99"))).thenReturn(Optional.of(product));

        component(null, null, "brxdis:pid", "p99").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("p99"));
    }

    // ── Stage 3: bean property used when URL param + document bean both absent ─

    @Test
    void pid_resolvedFromPageBeanProperty_whenNoUrlParamOrDocumentBean() {
        ProductSummary product = new ProductSummary("p-from-bean", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-from-bean"))).thenReturn(Optional.of(product));

        component(null, "p-from-bean", "brxdis:pid").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("p-from-bean"));
    }

    // ── URL param wins over document bean and page bean ───────────────────────

//    @Test
//    void pid_DocumentWins_overDocumentBeanAndPageBean() {
//        ProductSummary product = new ProductSummary("url-pid", "Test", null, null, null, null, Map.of());
//        when(discoveryService.fetchProduct(eq(request), eq("url-pid"))).thenReturn(Optional.of(product));
//
//        component("url-pid", "bean-pid", "brxdis:pid", "doc-pid").doBeforeRender(request, response);
//
//        verify(discoveryService).fetchProduct(eq(request), eq("url-pid"));
//        verify(discoveryService, never()).fetchProduct(eq(request), eq("doc-pid"));
//        verify(discoveryService, never()).fetchProduct(eq(request), eq("bean-pid"));
//    }

    // ── Custom PID property name is used when configured ─────────────────────

    @Test
    void pid_customPidProperty_usedWhenConfigured() {
        ProductSummary product = new ProductSummary("sku-42", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("sku-42"))).thenReturn(Optional.of(product));

        component(null, "sku-42", "commerce:sku").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("sku-42"));
    }

    // ── Custom URL param name used when configured ────────────────────────────

    @Test
    void pid_customUrlParam_usedWhenConfigured() {
        ProductSummary product = new ProductSummary("some-sku", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("some-sku"))).thenReturn(Optional.of(product));

        new TestableProductDetailComponent(discoveryService, "some-sku", null, "brxdis:pid", null, "sku")
                .doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("some-sku"));
    }

    // ── Document bean productId wins over page bean ───────────────────────────

    @Test
    void pid_resolvedFromDocumentBean_overridesPageBean() {
        ProductSummary product = new ProductSummary("doc-pid", "Test", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("doc-pid"))).thenReturn(Optional.of(product));

        component(null, "bean-pid", "brxdis:pid", "doc-pid").doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("doc-pid"));
        verify(discoveryService, never()).fetchProduct(eq(request), eq("bean-pid"));
    }

    // ── Bug #2: document with blank productId must not wipe valid URL param ──

    @Test
    void pid_urlParam_preservedWhenDocumentHasBlankProductId() {
        // document IS attached (documentPid = "") but has no productId set — must not override "url-pid"
        ProductSummary product = new ProductSummary("url-pid", "T", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("url-pid"))).thenReturn(Optional.of(product));

        new TestableProductDetailComponent(discoveryService, "url-pid", null, "brxdis:pid", "", "pid")
                .doBeforeRender(request, response);

        verify(discoveryService).fetchProduct(eq(request), eq("url-pid"));
    }

    // ── Bug #1: resolved pid must be exposed as a model attribute ────────────

    @Test
    void pid_exposedAsModelAttribute_forTemplateErrorMessage() {
        when(discoveryService.fetchProduct(eq(request), eq("bad-pid"))).thenReturn(Optional.empty());

        component("bad-pid").doBeforeRender(request, response);

        verify(request).setModel("pid", "bad-pid");
        verify(request).setAttribute("pid", "bad-pid");
    }

    // ── testable subclass ─────────────────────────────────────────────────────

    // ── band publication: marksBandPresent_whenProductFound ──────────────────

    @Test
    void marksBandPresent_whenProductFound() {
        ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        component("p-1").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.isProductDetailBandPresent(request, "default"));
    }

    @Test
    void marksBandPresent_whenProductNotFound() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        component("p-99").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.isProductDetailBandPresent(request, "default"));
    }

    @Test
    void putsToCacheWhenProductFound() {
        ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of());
        when(discoveryService.fetchProduct(eq(request), eq("p-1"))).thenReturn(Optional.of(product));

        component("p-1").doBeforeRender(request, response);

        Optional<ProductSummary> cached = DiscoveryRequestCache.getProductResult(request, "default");
        assertTrue(cached.isPresent());
        assertSame(product, cached.get());
    }

    @Test
    void doesNotPutToCache_whenProductNotFound() {
        when(discoveryService.fetchProduct(eq(request), eq("p-99"))).thenReturn(Optional.empty());

        component("p-99").doBeforeRender(request, response);

        assertTrue(DiscoveryRequestCache.getProductResult(request, "default").isEmpty());
    }

    // ── testable subclass ─────────────────────────────────────────────────────

    private static class TestableProductDetailComponent extends DiscoveryProductDetailComponent {

        private final HstDiscoveryService service;
        private final String urlPid;
        private final String beanPid;
        private final String pidProperty;
        private final String documentPid;
        private final String productUrlParam;

        TestableProductDetailComponent(HstDiscoveryService service, String urlPid,
                                       String beanPid, String pidProperty,
                                       String documentPid, String productUrlParam) {
            this.service = service;
            this.urlPid = urlPid;
            this.beanPid = beanPid;
            this.pidProperty = pidProperty;
            this.documentPid = documentPid;
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
                @Override public String getDocument() { return documentPid != null ? "test-doc" : ""; }
                @Override public String getProductPidProperty() { return pidProperty; }
                @Override public String getProductUrlParam() { return productUrlParam; }
                @Override public String getLabel() { return "default"; }
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

        @Override
        protected String resolvePidFromBean(HstRequest request, DiscoveryProductDetailComponentInfo info) {
            return beanPid;
        }
    }
}
