package org.bloomreach.forge.discovery.site.rest.visual.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VisualObject(
        @JsonProperty("id") int id,
        @JsonProperty("bbox") double[] bbox,
        @JsonProperty("objectType") String objectType
) {}
