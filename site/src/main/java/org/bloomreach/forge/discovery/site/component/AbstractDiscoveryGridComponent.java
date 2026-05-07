package org.bloomreach.forge.discovery.site.component;

import jakarta.servlet.http.HttpServletRequest;
import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySortOptionsProvider;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.site.HstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Shared base for the two Discovery Product Grid components.
 * Owns result-model population, empty-state setup, and URL construction helpers
 * so neither concrete subclass duplicates this logic.
 *
 * <p>Subclasses provide their own {@code doDiscoveryBeforeRender} implementation and
 * declare mode-specific {@code @ParametersInfo}.
 */
abstract class AbstractDiscoveryGridComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(AbstractDiscoveryGridComponent.class);

    /**
     * Populates the common result models (products, pagination, facets, URLs, sort).
     * Called by both concrete subclasses after a successful API response.
     */
    protected void populateResultModels(HstRequest request, SearchResponse searchResponse,
            boolean showFacets, boolean showPagination, boolean showSort,
            Map<String, String[]> params, Set<String> facetFields) {
        SearchResult result = searchResponse.result();
        PaginationModel pagination = new PaginationModel(result.total(), result.page(), result.pageSize());

        request.setModel(DiscoveryModelKeys.PRODUCTS, result.products());
        request.setModel(DiscoveryModelKeys.PAGINATION, pagination);
        request.setModel(DiscoveryModelKeys.STATS, searchResponse.metadata().stats());

        if (showFacets) {
            Map<String, Facet> facets = facetFields.isEmpty()
                    ? result.facets()
                    : result.facets().entrySet().stream()
                            .filter(e -> facetFields.contains(e.getKey()))
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
            request.setModel(DiscoveryModelKeys.FACETS, facets);
            request.setModel(DiscoveryModelKeys.FACET_URLS, buildFacetToggleUrls(params, facets));
            request.setModel(DiscoveryModelKeys.ACTIVE_FACETS, buildActiveFacetValues(params, facets));
            request.setModel(DiscoveryModelKeys.CLEAR_FILTERS_URL, buildClearAllUrl(params));
        }

        if (showPagination) {
            request.setModel(DiscoveryModelKeys.PAGE_URLS, buildPageUrls(params, pagination.totalPages()));
        }

        if (showSort) {
            request.setModel(DiscoveryModelKeys.SORT_URL, buildSortUrl(params));
            request.setModel(DiscoveryModelKeys.SORT_OPTIONS, resolveSortOptions());
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

    private List<Map<String, String>> resolveSortOptions() {
        try {
            if (HstServices.isAvailable() && HstServices.getComponentManager() != null) {
                DiscoveryConfigProvider provider = HstServices.getComponentManager()
                        .getComponent(DiscoveryConfigProvider.class.getName());
                if (provider != null) {
                    return toSortOptionMaps(provider.get().schemaConfig().sortOptions());
                }
            }
        } catch (Exception e) {
            log.warn("brxm-discovery: Could not resolve sort options from config - using defaults. Cause: {}", e.getMessage());
        }
        return toSortOptionMaps(ConfigDefaults.DEFAULT_SORT_OPTIONS);
    }

    static Set<String> parseFacetFields(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty))
                .collect(Collectors.toUnmodifiableSet());
    }

    private static List<Map<String, String>> toSortOptionMaps(List<String> entries) {
        return entries.stream()
                .map(e -> Map.of(
                        "value", DiscoverySortOptionsProvider.parseValue(e),
                        "label", DiscoverySortOptionsProvider.parseLabel(e)))
                .toList();
    }
}
