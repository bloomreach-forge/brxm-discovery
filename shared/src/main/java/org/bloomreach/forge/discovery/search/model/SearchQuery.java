package org.bloomreach.forge.discovery.search.model;

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
        String catalogName,
        List<String> statsFields,
        String segment,
        String efq
) {
    /** Backwards-compatible constructor (statsFields; no segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url,
                       String catalogName, List<String> statsFields) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName, statsFields, null, null);
    }

    /** Backwards-compatible constructor (no statsFields, segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url,
                       String catalogName) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName, List.of(), null, null);
    }

    /** Backwards-compatible constructor (no catalogName, statsFields, segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null, List.of(), null, null);
    }

    public SearchQuery withStatsFields(List<String> statsFields) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName, statsFields, segment, efq);
    }

    public SearchQuery withSegment(String segment) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName, statsFields, segment, efq);
    }

    public SearchQuery withEfq(String efq) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, catalogName, statsFields, segment, efq);
    }
}
