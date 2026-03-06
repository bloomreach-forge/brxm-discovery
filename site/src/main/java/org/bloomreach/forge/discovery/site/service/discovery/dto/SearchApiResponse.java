package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchApiResponse(
        @JsonProperty("response") ApiResponseBody response,
        @JsonProperty("facet_counts") FacetCounts facetCounts
) {}
