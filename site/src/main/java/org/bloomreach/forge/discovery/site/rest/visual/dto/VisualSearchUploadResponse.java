package org.bloomreach.forge.discovery.site.rest.visual.dto;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;

public record VisualSearchUploadResponse(
        String imageId,
        List<VisualObject> objects,
        List<ProductSummary> products
) {}
