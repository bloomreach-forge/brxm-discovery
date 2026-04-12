package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds relative query-string URLs for facet toggles, pagination, and sort controls.
 * All methods are pure functions that take the current request parameter map and return
 * pre-computed URL strings for use in templates.
 *
 * <p>Actor: UX / Editor - owns the navigation URL shape for search/category result pages.
 */
final class DiscoveryUrlBuilder {

    private DiscoveryUrlBuilder() {
    }

    /**
     * Builds facet toggle URLs for every facet value.
     * Active values produce removal URLs; inactive values produce addition URLs.
     * Changing any facet always resets {@code page} to avoid stale pagination.
     */
    static Map<String, Map<String, String>> buildFacetToggleUrls(
            Map<String, String[]> params, Map<String, Facet> facets) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        for (Facet facet : facets.values()) {
            String fp = "filter." + facet.name();
            Set<String> activeSet = Set.of(params.getOrDefault(fp, new String[0]));
            Map<String, String> valueUrls = new LinkedHashMap<>();
            for (FacetValue fv : facet.value()) {
                String fvName = fv.name();
                if (activeSet.contains(fvName)) {
                    valueUrls.put(fvName, buildUrl(params, null, null, fp, fvName));
                } else {
                    valueUrls.put(fvName, buildUrl(params, fp, fvName, null, null));
                }
            }
            result.put(facet.name(), valueUrls);
        }
        return result;
    }

    /** Extracts currently active filter values per facet name. */
    static Map<String, List<String>> buildActiveFacetValues(
            Map<String, String[]> params, Map<String, Facet> facets) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Facet facet : facets.values()) {
            String[] active = params.getOrDefault("filter." + facet.name(), new String[0]);
            if (active.length > 0) {
                result.put(facet.name(), List.of(active));
            }
        }
        return result;
    }

    /** Removes all {@code filter.*} params and resets {@code page}. */
    static String buildClearAllUrl(Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String k = e.getKey();
            if ("page".equals(k) || k.startsWith("filter.")) continue;
            for (String v : e.getValue()) {
                if (!first) sb.append('&');
                sb.append(encode(k)).append('=').append(encode(v));
                first = false;
            }
        }
        return sb.toString();
    }

    /** Builds page URLs for every 0-indexed page in [0, totalPages). Page 0 omits the param. */
    static Map<String, String> buildPageUrls(Map<String, String[]> params, int totalPages) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int p = 0; p < totalPages; p++) {
            result.put(String.valueOf(p), buildPageUrl(params, p));
        }
        return result;
    }

    /** Base URL for sort switching: strips {@code sort} and {@code page}. Template appends {@code &sort=value}. */
    static String buildSortUrl(Map<String, String[]> params) {
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String k = e.getKey();
            if ("sort".equals(k) || "page".equals(k)) continue;
            for (String v : e.getValue()) {
                if (!first) sb.append('&');
                sb.append(encode(k)).append('=').append(encode(v));
                first = false;
            }
        }
        return sb.toString();
    }

    /**
     * Builds a query string from {@code params}, skipping {@code page},
     * optionally removing one key=value pair, and optionally appending one new pair.
     */
    private static String buildUrl(Map<String, String[]> params,
            String addKey, String addVal,
            String removeKey, String removeVal) {
        StringBuilder sb = new StringBuilder("?");
        boolean first = true;
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            String k = e.getKey();
            if ("page".equals(k)) continue;
            for (String v : e.getValue()) {
                if (k.equals(removeKey) && v.equals(removeVal)) continue;
                if (!first) sb.append('&');
                sb.append(encode(k)).append('=').append(encode(v));
                first = false;
            }
        }
        if (addKey != null && !addKey.isEmpty()) {
            if (!first) sb.append('&');
            sb.append(encode(addKey)).append('=').append(encode(addVal));
        }
        return sb.toString();
    }

    private static String buildPageUrl(Map<String, String[]> params, int page) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String[]> e : params.entrySet()) {
            if ("page".equals(e.getKey())) continue;
            for (String v : e.getValue()) {
                parts.add(encode(e.getKey()) + "=" + encode(v));
            }
        }
        if (page > 0) {
            parts.add("page=" + page);
        }
        return "?" + String.join("&", parts);
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
