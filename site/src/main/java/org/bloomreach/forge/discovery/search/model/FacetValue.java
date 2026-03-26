package org.bloomreach.forge.discovery.search.model;

public record FacetValue(
        String name,
        long count,
        String catId,
        String crumb,
        String treePath,
        String parent,
        Double start,
        Double end
) {}
