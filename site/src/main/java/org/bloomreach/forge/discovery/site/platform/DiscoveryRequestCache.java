package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

import java.util.List;
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

    public static Optional<SearchResult> getSearchResult(HstRequest request) {
        return Optional.ofNullable((SearchResult) ctx(request).getAttribute(ATTR + ".searchResult"));
    }

    public static void putSearchResult(HstRequest request, SearchResult result) {
        ctx(request).setAttribute(ATTR + ".searchResult", result);
    }

    public static Optional<SearchResult> getCategoryResult(HstRequest request) {
        return Optional.ofNullable((SearchResult) ctx(request).getAttribute(ATTR + ".categoryResult"));
    }

    public static void putCategoryResult(HstRequest request, SearchResult result) {
        ctx(request).setAttribute(ATTR + ".categoryResult", result);
    }

    @SuppressWarnings("unchecked")
    public static Optional<List<ProductSummary>> getRecommendations(HstRequest request, String widgetId) {
        return Optional.ofNullable((List<ProductSummary>) ctx(request).getAttribute(ATTR + ".recs." + widgetId));
    }

    public static void putRecommendations(HstRequest request, String widgetId, List<ProductSummary> products) {
        ctx(request).setAttribute(ATTR + ".recs." + widgetId, products);
    }

    private static HstRequestContext ctx(HstRequest request) {
        return request.getRequestContext();
    }
}
