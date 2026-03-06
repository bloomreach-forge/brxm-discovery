package org.bloomreach.forge.discovery.site.service.discovery.search.model;

import java.math.BigDecimal;
import java.util.Map;

public record ProductSummary(
        String id,
        String title,
        String url,
        String imageUrl,
        BigDecimal price,
        String currency,
        Map<String, Object> attributes
) {
}
