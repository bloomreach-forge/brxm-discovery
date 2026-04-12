package org.bloomreach.forge.discovery.site.component;

import jakarta.servlet.http.HttpServletRequest;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

import java.util.List;
import java.util.Map;

/**
 * Shared base for the two Discovery Product Grid components.
 * Owns result-model population, empty-state setup, and URL construction helpers
 * so neither concrete subclass duplicates this logic.
 *
 * <p>Subclasses provide their own {@code doBeforeRender} implementation and
 * declare mode-specific {@code @ParametersInfo}.
 */
abstract class AbstractDiscoveryGridComponent extends AbstractDiscoveryComponent {

    /**
     * Populates the common result models (products, pagination, facets, URLs, sort).
     * Called by both concrete subclasses after a successful API response.
     */
    protected void populateResultModels(HstRequest request, SearchResponse searchResponse,
            boolean showFacets, boolean showPagination, boolean showSort,
            Map<String, String[]> params) {
        SearchResult result = searchResponse.result();
        PaginationModel pagination = new PaginationModel(result.total(), result.page(), result.pageSize());

        request.setModel(DiscoveryModelKeys.PRODUCTS, result.products());
        request.setModel(DiscoveryModelKeys.PAGINATION, pagination);
        request.setModel(DiscoveryModelKeys.STATS, searchResponse.metadata().stats());

        if (showFacets) {
            request.setModel(DiscoveryModelKeys.FACETS, result.facets());
            request.setModel(DiscoveryModelKeys.FACET_URLS, buildFacetToggleUrls(params, result.facets()));
            request.setModel(DiscoveryModelKeys.ACTIVE_FACETS, buildActiveFacetValues(params, result.facets()));
            request.setModel(DiscoveryModelKeys.CLEAR_FILTERS_URL, buildClearAllUrl(params));
        }

        if (showPagination) {
            request.setModel(DiscoveryModelKeys.PAGE_URLS, buildPageUrls(params, pagination.totalPages()));
        }

        if (showSort) {
            request.setModel(DiscoveryModelKeys.SORT_URL, buildSortUrl(params));
        }
    }

    protected void setEmptyState(HstRequest request) {
        request.setModel(DiscoveryModelKeys.PRODUCTS, null);
        request.setModel(DiscoveryModelKeys.PAGINATION, new PaginationModel(0L, 0, 0));
    }

    /** Reads servlet parameters. Overridable in tests. */
    protected Map<String, String[]> getServletParameters(HstRequest request) {
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) return Map.of();
        HttpServletRequest sr = ctx.getServletRequest();
        if (sr == null) return Map.of();
        return sr.getParameterMap();
    }

    static Map<String, Map<String, String>> buildFacetToggleUrls(
            Map<String, String[]> params, Map<String, Facet> facets) {
        return DiscoveryUrlBuilder.buildFacetToggleUrls(params, facets);
    }

    static Map<String, List<String>> buildActiveFacetValues(
            Map<String, String[]> params, Map<String, Facet> facets) {
        return DiscoveryUrlBuilder.buildActiveFacetValues(params, facets);
    }

    static String buildClearAllUrl(Map<String, String[]> params) {
        return DiscoveryUrlBuilder.buildClearAllUrl(params);
    }

    static Map<String, String> buildPageUrls(Map<String, String[]> params, int totalPages) {
        return DiscoveryUrlBuilder.buildPageUrls(params, totalPages);
    }

    static String buildSortUrl(Map<String, String[]> params) {
        return DiscoveryUrlBuilder.buildSortUrl(params);
    }
}
