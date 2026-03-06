package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.util.List;
import java.util.Map;

public record SearchQuery(
        String query,
        int page,
        int pageSize,
        String sort,
        Map<String, List<String>> filters,
        String brUid2,
        String refUrl,
        String url,
        String catalogName
) {
    /** Backwards-compatible constructor (no catalogName). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null);
    }
}
