package org.bloomreach.forge.discovery.site.rest.visual.dto;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;

public record VisualSearchResult(List<ProductSummary> products) {}
