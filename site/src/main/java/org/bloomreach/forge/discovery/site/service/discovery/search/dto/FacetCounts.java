package org.bloomreach.forge.discovery.site.service.discovery.search.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FacetCounts(
        @JsonProperty("facets") List<FacetFieldDto> facets
) {}
