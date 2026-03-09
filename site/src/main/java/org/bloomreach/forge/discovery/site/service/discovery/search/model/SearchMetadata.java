package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.util.List;
import java.util.Map;

public record SearchMetadata(
        Map<String, FieldStats> stats,
        List<String> didYouMean,
        String autoCorrectQuery,
        String redirectUrl,
        String redirectQuery,
        Campaign campaign
) {

    /** Backwards-compatible 5-arg constructor (no campaign). */
    public SearchMetadata(Map<String, FieldStats> stats, List<String> didYouMean,
                          String autoCorrectQuery, String redirectUrl, String redirectQuery) {
        this(stats, didYouMean, autoCorrectQuery, redirectUrl, redirectQuery, null);
    }

    public static SearchMetadata empty() {
        return new SearchMetadata(Map.of(), null, null, null, null, null);
    }
}
