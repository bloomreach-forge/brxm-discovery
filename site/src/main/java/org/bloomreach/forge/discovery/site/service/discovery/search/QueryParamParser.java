package org.bloomreach.forge.discovery.site.service.discovery.search;

import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryParamParser {

    private QueryParamParser() {}

    /**
     * Parses HTTP request parameters into a {@link SearchQuery}.
     * Falls back to config defaults for pageSize and sort.
     *
     * @param paramProvider supplier of request parameter value
     * @param settings      current channel's DiscoverySettings
     */
    public static SearchQuery toSearchQuery(RequestParamProvider paramProvider, DiscoverySettings settings,
                                            String brUid2, String refUrl, String url) {
        return toSearchQuery(paramProvider, settings, 0, null, null, brUid2, refUrl, url);
    }

    /**
     * Parses HTTP request parameters into a {@link SearchQuery} with explicit component-level fallbacks.
     * Priority: URL param → pageSizeFallback/sortFallback → config defaults.
     *
     * @param paramProvider    supplier of request parameter value
     * @param settings         current channel's DiscoverySettings
     * @param pageSizeFallback component-configured page size; {@code <= 0} falls through to config default
     * @param sortFallback     component-configured sort; blank/null falls through to config default
     */
    public static SearchQuery toSearchQuery(RequestParamProvider paramProvider, DiscoverySettings settings,
                                            int pageSizeFallback, String sortFallback,
                                            String brUid2, String refUrl, String url) {
        return toSearchQuery(paramProvider, settings, pageSizeFallback, sortFallback, null, brUid2, refUrl, url);
    }

    public static SearchQuery toSearchQuery(RequestParamProvider paramProvider, DiscoverySettings settings,
                                            int pageSizeFallback, String sortFallback, String catalogName,
                                            String brUid2, String refUrl, String url) {
        String searchTerm = paramProvider.getParameter("q");
        int page = Math.max(0, parseIntOrDefault(paramProvider.getParameter("page"), 1) - 1);
        int effectivePageSizeFallback = pageSizeFallback > 0 ? pageSizeFallback : settings.defaultPageSize();
        int pageSize = parseIntOrDefault(paramProvider.getParameter("pageSize"), effectivePageSizeFallback);
        String effectiveSortFallback = (sortFallback != null && !sortFallback.isBlank())
                ? sortFallback : settings.defaultSort();
        String sort = firstNonBlank(paramProvider.getParameter("sort"), effectiveSortFallback);
        Map<String, List<String>> filters = parseFilters(paramProvider.getParameterMap());
        String segment = paramProvider.getParameter("seg");
        return new SearchQuery(searchTerm, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName,
                List.of(), segment != null && !segment.isBlank() ? segment : null, null);
    }

    /**
     * Parses HTTP request parameters into a {@link CategoryQuery}.
     *
     * @param categoryId    resolved category ID (from sitemap or component param)
     * @param paramProvider supplier of request parameter value
     * @param settings      current channel's DiscoverySettings
     * @param brUid2        value of the _br_uid_2 browser cookie
     * @param refUrl        Referer header value (or fallback page URL)
     * @param url           absolute URL of the current page
     */
    public static CategoryQuery toCategoryQuery(String categoryId,
                                                RequestParamProvider paramProvider,
                                                DiscoverySettings settings,
                                                String brUid2, String refUrl, String url) {
        return toCategoryQuery(categoryId, paramProvider, settings, 0, null, brUid2, refUrl, url);
    }

    /**
     * Parses HTTP request parameters into a {@link CategoryQuery} with explicit component-level fallbacks.
     * Priority: URL param → pageSizeFallback/sortFallback → config defaults.
     *
     * @param categoryId       resolved category ID (from sitemap or component param)
     * @param paramProvider    supplier of request parameter value
     * @param settings         current channel's DiscoverySettings
     * @param pageSizeFallback component-configured page size; {@code <= 0} falls through to config default
     * @param sortFallback     component-configured sort; blank/null falls through to config default
     * @param brUid2           value of the _br_uid_2 browser cookie
     * @param refUrl           Referer header value (or fallback page URL)
     * @param url              absolute URL of the current page
     */
    public static CategoryQuery toCategoryQuery(String categoryId,
                                                RequestParamProvider paramProvider,
                                                DiscoverySettings settings,
                                                int pageSizeFallback, String sortFallback,
                                                String brUid2, String refUrl, String url) {
        int page = Math.max(0, parseIntOrDefault(paramProvider.getParameter("page"), 1) - 1);
        int effectivePageSizeFallback = pageSizeFallback > 0 ? pageSizeFallback : settings.defaultPageSize();
        int pageSize = parseIntOrDefault(paramProvider.getParameter("pageSize"), effectivePageSizeFallback);
        String effectiveSortFallback = (sortFallback != null && !sortFallback.isBlank())
                ? sortFallback : settings.defaultSort();
        String sort = firstNonBlank(paramProvider.getParameter("sort"), effectiveSortFallback);
        Map<String, List<String>> filters = parseFilters(paramProvider.getParameterMap());
        String segment = paramProvider.getParameter("seg");
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url,
                List.of(), segment != null && !segment.isBlank() ? segment : null, null);
    }

    /**
     * Simple interface abstracting HST/servlet request parameter access so the
     * parser can be tested without a full HST request.
     */
    public interface RequestParamProvider {
        String getParameter(String name);
        Map<String, String[]> getParameterMap();
    }

    private static Map<String, List<String>> parseFilters(Map<String, String[]> paramMap) {
        Map<String, List<String>> filters = new HashMap<>();
        for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("filter.")) {
                String attribute = key.substring("filter.".length());
                List<String> values = new ArrayList<>();
                for (String v : entry.getValue()) {
                    if (v != null && !v.isBlank()) {
                        values.add(v);
                    }
                }
                if (!values.isEmpty()) {
                    filters.put(attribute, values);
                }
            }
        }
        return filters;
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String firstNonBlank(String first, String fallback) {
        return (first != null && !first.isBlank()) ? first : fallback;
    }
}
