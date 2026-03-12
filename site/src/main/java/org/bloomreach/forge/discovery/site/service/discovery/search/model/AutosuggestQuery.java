package org.bloomreach.forge.discovery.search.model;

public record AutosuggestQuery(
        String query,
        int limit,
        String catalogViews,
        String brUid2,
        String refUrl,
        String url
) {
    public AutosuggestQuery(String query, int limit) {
        this(query, limit, null, null, null, null);
    }
}
