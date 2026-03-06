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

    // ── Band-aware overloads ──────────────────────────────────────────────────

    public static Optional<SearchResult> getSearchResult(HstRequest request, String band) {
        return Optional.ofNullable((SearchResult) ctx(request).getAttribute(ATTR + ".searchResult." + band));
    }

    public static void putSearchResult(HstRequest request, String band, SearchResult result) {
        ctx(request).setAttribute(ATTR + ".searchResult." + band, result);
    }

    public static Optional<SearchResult> getCategoryResult(HstRequest request, String band) {
        return Optional.ofNullable((SearchResult) ctx(request).getAttribute(ATTR + ".categoryResult." + band));
    }

    public static void putCategoryResult(HstRequest request, String band, SearchResult result) {
        ctx(request).setAttribute(ATTR + ".categoryResult." + band, result);
    }

    // ── Band-presence markers (set by data components before any early return) ──
    //
    // View components use these to distinguish "band not wired up on this page" (show warning)
    // from "band connected but no results yet, e.g. no query typed" (silent empty state).
    // Search and category markers are kept separate so a category component on the same page
    // cannot satisfy a view component that expects a search data source and vice-versa.

    public static void markSearchBandPresent(HstRequest request, String band) {
        ctx(request).setAttribute(ATTR + ".band.search." + band, Boolean.TRUE);
    }

    public static boolean isSearchBandPresent(HstRequest request, String band) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".band.search." + band));
    }

    public static void markCategoryBandPresent(HstRequest request, String band) {
        ctx(request).setAttribute(ATTR + ".band.category." + band, Boolean.TRUE);
    }

    public static boolean isCategoryBandPresent(HstRequest request, String band) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".band.category." + band));
    }

    // ── No-band overloads delegate to "default" band (backward compat) ────────

    public static Optional<SearchResult> getSearchResult(HstRequest request) {
        return getSearchResult(request, "default");
    }

    public static void putSearchResult(HstRequest request, SearchResult result) {
        putSearchResult(request, "default", result);
    }

    public static Optional<SearchResult> getCategoryResult(HstRequest request) {
        return getCategoryResult(request, "default");
    }

    public static void putCategoryResult(HstRequest request, SearchResult result) {
        putCategoryResult(request, "default", result);
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
