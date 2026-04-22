package org.bloomreach.forge.discovery.search.model;

import java.util.List;
import java.util.Map;

public record CategoryQuery(
        String categoryId,
        int page,
        int pageSize,
        String sort,
        Map<String, List<String>> filters,
        String brUid2,
        String refUrl,
        String url,
        String origRefUrl,
        List<String> statsFields,
        String segment,
        String efq,
        Map<String, RangeSelection> rangeFilters,
        String fields
) {
    /** Backwards-compatible constructor (statsFields; no segment, efq). */
    public CategoryQuery(String categoryId, int page, int pageSize, String sort,
                         Map<String, List<String>> filters,
                         String brUid2, String refUrl, String url, List<String> statsFields) {
        this(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, null, statsFields, null, null, Map.of(), null);
    }

    /** Backwards-compatible constructor (no statsFields, segment, efq). */
    public CategoryQuery(String categoryId, int page, int pageSize, String sort,
                         Map<String, List<String>> filters,
                         String brUid2, String refUrl, String url) {
        this(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, null, List.of(), null, null, Map.of(), null);
    }

    /** Backwards-compatible constructor (pre-origRefUrl canonical signature). */
    public CategoryQuery(String categoryId, int page, int pageSize, String sort,
                         Map<String, List<String>> filters,
                         String brUid2, String refUrl, String url,
                         List<String> statsFields, String segment, String efq) {
        this(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, null, statsFields, segment, efq, Map.of(), null);
    }

    public CategoryQuery withStatsFields(List<String> statsFields) {
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, statsFields, segment, efq, rangeFilters, fields);
    }

    public CategoryQuery withSegment(String segment) {
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, statsFields, segment, efq, rangeFilters, fields);
    }

    public CategoryQuery withEfq(String efq) {
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, statsFields, segment, efq, rangeFilters, fields);
    }

    public CategoryQuery withRangeFilters(Map<String, RangeSelection> rangeFilters) {
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, statsFields, segment, efq, rangeFilters, fields);
    }

    public CategoryQuery withFields(String fields) {
        return new CategoryQuery(categoryId, page, pageSize, sort, filters, brUid2, refUrl, url, origRefUrl, statsFields, segment, efq, rangeFilters, fields);
    }
}
