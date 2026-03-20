package org.bloomreach.forge.discovery.site.platform;

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
        String label,
        List<String> statsFields,
        String segment,
        String efq
) {
    /** Default options: component page-size of 0 (→ falls through to URL param or coded default), no label override. */
    public static SearchRequestOptions defaults() {
        return new SearchRequestOptions(0, null, null, "default", List.of(), null, null);
    }

    /** Convenience constructor for label + pageSize — the most common override pair. */
    public static SearchRequestOptions of(String label, int pageSize) {
        return new SearchRequestOptions(pageSize, null, null, label, List.of(), null, null);
    }
}
