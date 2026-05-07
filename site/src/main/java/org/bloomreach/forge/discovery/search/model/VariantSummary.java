package org.bloomreach.forge.discovery.search.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record VariantSummary(
        String skuId,
        String color,
        String colorGroup,
        String size,
        BigDecimal price,
        BigDecimal salePrice,
        List<String> thumbnailImages,
        List<String> largeImages,
        List<String> swatchImages,
        Map<String, Object> attributes
) {}
