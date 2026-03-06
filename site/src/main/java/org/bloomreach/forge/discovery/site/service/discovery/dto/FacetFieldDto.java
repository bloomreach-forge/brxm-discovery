package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FacetFieldDto(
        @JsonProperty("name") String name,
        @JsonProperty("type") String type,
        @JsonProperty("value") List<FacetValueDto> value
) {}
