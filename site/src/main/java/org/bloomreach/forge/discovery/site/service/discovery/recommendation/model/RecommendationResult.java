package org.bloomreach.forge.discovery.site.service.discovery.recommendation.model;

import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;

import java.util.List;

/**
 * Domain result for a recommendation widget call.
 * Carries the widget result ID ({@code wrid}) from Pathways metadata — needed for accurate
 * server-side pixel events — alongside the product list.
 */
public record RecommendationResult(String widgetResultId, List<ProductSummary> products) {

    /**
     * Convenience factory for v1 responses and test helpers where {@code wrid} is unavailable.
     */
    public static RecommendationResult of(List<ProductSummary> products) {
        return new RecommendationResult(null, products);
    }
}
