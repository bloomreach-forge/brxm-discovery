package org.bloomreach.forge.discovery.site.platform;

import java.util.List;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryRequestCacheTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;

    private final Map<String, Object> attrs = new HashMap<>();
    private final ProductSummary product = new ProductSummary("p-1", "T", null, null, null, null, Map.of(), List.of());

    @BeforeEach
    void setUp() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> {
            attrs.put((String) inv.getArgument(0), inv.getArgument(1));
            return null;
        }).when(requestContext).setAttribute(anyString(), any());
    }

    // ── null context guard ───────────────────────────────────────────────────

    @Test
    void ctx_nullRequestContext_throwsIllegalState() {
        when(request.getRequestContext()).thenReturn(null);
        assertThrows(IllegalStateException.class,
                () -> DiscoveryRequestCache.isProductDetailRendered(request));
    }

    // ── product detail (no label) ────────────────────────────────────────────

    @Test
    void isProductDetailRendered_returnsFalse_beforeMark() {
        assertFalse(DiscoveryRequestCache.isProductDetailRendered(request));
    }

    @Test
    void markProductDetailRendered_roundTrips() {
        DiscoveryRequestCache.markProductDetailRendered(request);
        assertTrue(DiscoveryRequestCache.isProductDetailRendered(request));
    }

    @Test
    void getProductResult_missing_returnsEmpty() {
        assertTrue(DiscoveryRequestCache.getProductResult(request).isEmpty());
    }

    @Test
    void putAndGet_productResult_roundTrips() {
        DiscoveryRequestCache.putProductResult(request, product);
        Optional<ProductSummary> got = DiscoveryRequestCache.getProductResult(request);
        assertTrue(got.isPresent());
        assertSame(product, got.get());
    }

    @Test
    void productDetailMarker_independentOfResult() {
        // Marker present, no result → valid state (PDP ran, but no product found)
        DiscoveryRequestCache.markProductDetailRendered(request);
        assertTrue(DiscoveryRequestCache.isProductDetailRendered(request));
        assertTrue(DiscoveryRequestCache.getProductResult(request).isEmpty());
    }

    // ── fetched product by PID ───────────────────────────────────────────────

    @Test
    void putAndGet_fetchedProductByPid_roundTrips() {
        DiscoveryRequestCache.putFetchedProduct(request, "sku-1", product);

        Optional<ProductSummary> got = DiscoveryRequestCache.getFetchedProduct(request, "sku-1");

        assertTrue(got.isPresent());
        assertSame(product, got.get());
    }

    @Test
    void fetchedProducts_areIndependentByPid() {
        ProductSummary p2 = new ProductSummary("p-2", "U", null, null, null, null, Map.of(), List.of());
        DiscoveryRequestCache.putFetchedProduct(request, "sku-1", product);
        DiscoveryRequestCache.putFetchedProduct(request, "sku-2", p2);

        assertSame(product, DiscoveryRequestCache.getFetchedProduct(request, "sku-1").orElseThrow());
        assertSame(p2, DiscoveryRequestCache.getFetchedProduct(request, "sku-2").orElseThrow());
        assertTrue(DiscoveryRequestCache.getFetchedProduct(request, "missing").isEmpty());
    }

}
