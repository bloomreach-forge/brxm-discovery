package org.bloomreach.forge.discovery.search.model;

import java.util.List;
import java.util.Map;

public record SearchMetadata(
        Map<String, FieldStats> stats,
        List<String> didYouMean,
        String autoCorrectQuery,
        String redirectUrl,
        String redirectQuery,
        Campaign campaign,
        String categoryName
) {

    /** Backwards-compatible 6-arg constructor (no categoryName). */
    public SearchMetadata(Map<String, FieldStats> stats, List<String> didYouMean,
                          String autoCorrectQuery, String redirectUrl, String redirectQuery,
                          Campaign campaign) {
        this(stats, didYouMean, autoCorrectQuery, redirectUrl, redirectQuery, campaign, null);
    }

    /** Backwards-compatible 5-arg constructor (no campaign, no categoryName). */
    public SearchMetadata(Map<String, FieldStats> stats, List<String> didYouMean,
                          String autoCorrectQuery, String redirectUrl, String redirectQuery) {
        this(stats, didYouMean, autoCorrectQuery, redirectUrl, redirectQuery, null, null);
    }

    public static SearchMetadata empty() {
        return new SearchMetadata(Map.of(), null, null, null, null, null, null);
    }
}
