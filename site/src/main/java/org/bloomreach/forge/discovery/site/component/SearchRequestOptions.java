package org.bloomreach.forge.discovery.site.component;

import java.util.List;

/**
 * Value object for optional per-request search and browse parameters.
 * Replaces the overloaded {@code search(HstRequest, int, String, ...)} family with a single
 * named-parameter carrier, making call sites self-documenting and eliminating positional confusion.
 *
 * <p>Use {@link #defaults()} as a starting point and override specific fields via the canonical
 * all-args constructor.
 */
public record SearchRequestOptions(
        int pageSize,
        String sort,
        String catalogName,
        List<String> statsFields,
        String segment,
        String efq
) {
    /** Default options: component page-size of 0 (→ falls through to URL param or coded default). */
    public static SearchRequestOptions defaults() {
        return new SearchRequestOptions(0, null, null, List.of(), null, null);
    }

    /** Convenience constructor for a custom page-size with all other fields defaulted. */
    public static SearchRequestOptions of(int pageSize) {
        return new SearchRequestOptions(pageSize, null, null, List.of(), null, null);
    }
}
