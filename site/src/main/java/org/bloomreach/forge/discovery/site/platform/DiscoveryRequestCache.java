package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

import java.util.Optional;

/**
 * Request-scoped deduplication cache for Discovery API results.
 * <p>
 * Uses {@link HstRequestContext#setAttribute} so attributes are shared across all sibling
 * HST components in a single page render. {@code HstRequest.setAttribute} is namespace-scoped
 * per component window and is NOT visible to siblings — making it unsuitable here.
 */
public final class DiscoveryRequestCache {

    private static final String ATTR = "org.bloomreach.forge.discovery.requestCache";

    private DiscoveryRequestCache() {}

    // ── Product detail ────────────────────────────────────────────────────────
    //
    // PDP component writes the resolved product so downstream recommendation
    // components can read the PID without needing a URL param.

    public static void markProductDetailRendered(HstRequest request) {
        ctx(request).setAttribute(ATTR + ".productDetailPresent", Boolean.TRUE);
    }

    public static boolean isProductDetailRendered(HstRequest request) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".productDetailPresent"));
    }

    public static void putProductResult(HstRequest request, ProductSummary product) {
        ctx(request).setAttribute(ATTR + ".productDetailResult", product);
    }

    public static Optional<ProductSummary> getProductResult(HstRequest request) {
        return Optional.ofNullable((ProductSummary) ctx(request).getAttribute(ATTR + ".productDetailResult"));
    }

    // ── Fetched product by PID (dedup within render) ──────────────────────────

    public static void putFetchedProduct(HstRequest request, String pid, ProductSummary product) {
        ctx(request).setAttribute(ATTR + ".productLookup." + pid, product);
    }

    public static Optional<ProductSummary> getFetchedProduct(HstRequest request, String pid) {
        return Optional.ofNullable((ProductSummary) ctx(request).getAttribute(ATTR + ".productLookup." + pid));
    }

    private static HstRequestContext ctx(HstRequest request) {
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) {
            throw new IllegalStateException(
                    "HstRequestContext is null — cache methods must only be called within an active HST request");
        }
        return ctx;
    }
}
