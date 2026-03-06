package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.util.List;

public record AutosuggestResult(
        String originalQuery,
        List<String> querySuggestions,
        List<AttributeSuggestion> attributeSuggestions,
        List<ProductSummary> productSuggestions
) {
    public record AttributeSuggestion(
            String name,
            String value,
            String attributeType
    ) {}
}
