package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
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

    // ── Label-aware overloads ──────────────────────────────────────────────────

    public static Optional<SearchResponse> getSearchResponse(HstRequest request, String label) {
        return Optional.ofNullable((SearchResponse) ctx(request).getAttribute(ATTR + ".searchResult." + label));
    }

    public static void putSearchResponse(HstRequest request, String label, SearchResponse response) {
        ctx(request).setAttribute(ATTR + ".searchResult." + label, response);
    }

    public static Optional<SearchResponse> getCategoryResponse(HstRequest request, String label) {
        return Optional.ofNullable((SearchResponse) ctx(request).getAttribute(ATTR + ".categoryResult." + label));
    }

    public static void putCategoryResponse(HstRequest request, String label, SearchResponse response) {
        ctx(request).setAttribute(ATTR + ".categoryResult." + label, response);
    }

    // ── Label-presence markers (set by data components before any early return) ──
    //
    // View components use these to distinguish "label not wired up on this page" (show warning)
    // from "label connected but no results yet, e.g. no query typed" (silent empty state).
    // Search and category markers are kept separate so a category component on the same page
    // cannot satisfy a view component that expects a search data source and vice-versa.

    public static void markSearchBandPresent(HstRequest request, String label) {
        ctx(request).setAttribute(ATTR + ".label.search." + label, Boolean.TRUE);
    }

    public static boolean isSearchBandPresent(HstRequest request, String label) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".label.search." + label));
    }

    public static void markCategoryBandPresent(HstRequest request, String label) {
        ctx(request).setAttribute(ATTR + ".label.category." + label, Boolean.TRUE);
    }

    public static boolean isCategoryBandPresent(HstRequest request, String label) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".label.category." + label));
    }

    // ── No-band overloads delegate to "default" band (backward compat) ────────

    public static Optional<SearchResponse> getSearchResponse(HstRequest request) {
        return getSearchResponse(request, "default");
    }

    public static void putSearchResponse(HstRequest request, SearchResponse response) {
        putSearchResponse(request, "default", response);
    }

    public static Optional<SearchResponse> getCategoryResponse(HstRequest request) {
        return getCategoryResponse(request, "default");
    }

    public static void putCategoryResponse(HstRequest request, SearchResponse response) {
        putCategoryResponse(request, "default", response);
    }

    // ── Product detail label ──────────────────────────────────────────────────
    //
    // PDP components write the resolved product to a named label so downstream
    // recommendation components can read the PID without needing a URL param.

    public static void putProductResult(HstRequest request, String label, ProductSummary product) {
        ctx(request).setAttribute(ATTR + ".productDetailResult." + label, product);
    }

    public static Optional<ProductSummary> getProductResult(HstRequest request, String label) {
        return Optional.ofNullable((ProductSummary) ctx(request).getAttribute(ATTR + ".productDetailResult." + label));
    }

    public static void markProductDetailBandPresent(HstRequest request, String label) {
        ctx(request).setAttribute(ATTR + ".label.productDetail." + label, Boolean.TRUE);
    }

    public static boolean isProductDetailBandPresent(HstRequest request, String label) {
        return Boolean.TRUE.equals(ctx(request).getAttribute(ATTR + ".label.productDetail." + label));
    }

    // ── Label-aware recommendation overloads ────────────────────────────────

    public static Optional<RecommendationResult> getRecommendations(HstRequest request, String label, String widgetId) {
        return Optional.ofNullable((RecommendationResult) ctx(request).getAttribute(ATTR + ".recs." + label + "." + widgetId));
    }

    public static void putRecommendations(HstRequest request, String label, String widgetId, RecommendationResult result) {
        ctx(request).setAttribute(ATTR + ".recs." + label + "." + widgetId, result);
    }

    // ── No-band recommendation overloads → "default" (backward compat) ───

    public static Optional<RecommendationResult> getRecommendations(HstRequest request, String widgetId) {
        return getRecommendations(request, "default", widgetId);
    }

    public static void putRecommendations(HstRequest request, String widgetId, RecommendationResult result) {
        putRecommendations(request, "default", widgetId, result);
    }

    private static HstRequestContext ctx(HstRequest request) {
        return request.getRequestContext();
    }
}
