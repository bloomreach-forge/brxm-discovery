package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.util.List;
import java.util.Map;

public record SearchResult(
        List<ProductSummary> products,
        long total,
        int page,
        int pageSize,
        Map<String, Facet> facets
) {
}
