package org.bloomreach.forge.discovery.search.model;

import java.util.List;

public record Facet(
        String name,
        String type,
        List<FacetValue> value
) {}
