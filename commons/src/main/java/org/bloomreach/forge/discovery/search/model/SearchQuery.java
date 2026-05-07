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
        String origRefUrl,
        String catalogName,
        List<String> statsFields,
        String segment,
        String efq,
        Map<String, RangeSelection> rangeFilters,
        String fields,
        String viewId
) {
    /** Backwards-compatible constructor (statsFields; no segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url,
                       String catalogName, List<String> statsFields) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null, catalogName, statsFields, null, null, Map.of(), null, null);
    }

    /** Backwards-compatible constructor (pre-origRefUrl canonical signature). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url,
                       String catalogName, List<String> statsFields,
                       String segment, String efq) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null, catalogName, statsFields, segment, efq, Map.of(), null, null);
    }

    /** Backwards-compatible constructor (no statsFields, segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url,
                       String catalogName) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null, catalogName, List.of(), null, null, Map.of(), null, null);
    }

    /** Backwards-compatible constructor (no catalogName, statsFields, segment, efq). */
    public SearchQuery(String query, int page, int pageSize, String sort,
                       Map<String, List<String>> filters,
                       String brUid2, String refUrl, String url) {
        this(query, page, pageSize, sort, filters, brUid2, refUrl, url, null, null, List.of(), null, null, Map.of(), null, null);
    }

    public SearchQuery withStatsFields(List<String> statsFields) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }

    public SearchQuery withSegment(String segment) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }

    public SearchQuery withEfq(String efq) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }

    public SearchQuery withRangeFilters(Map<String, RangeSelection> rangeFilters) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }

    public SearchQuery withFields(String fields) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }

    public SearchQuery withViewId(String viewId) {
        return new SearchQuery(query, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, catalogName, statsFields, segment, efq, rangeFilters, fields, viewId);
    }
}
