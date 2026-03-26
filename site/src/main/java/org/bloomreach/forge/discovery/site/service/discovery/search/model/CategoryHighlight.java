package org.bloomreach.forge.discovery.site.service.discovery.search.model;

/**
 * Clean record representing a curated category tile in the Category Highlight component.
 * Replaces direct exposure of {@code DiscoveryCategoryBean} in the page model.
 * {@code href} is intentionally excluded — the frontend owns routing.
 */
public record CategoryHighlight(
        String categoryId,
        String displayName,
        int productPreviewCount
) {}
