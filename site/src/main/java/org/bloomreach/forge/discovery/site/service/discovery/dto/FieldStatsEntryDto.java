package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldStatsEntryDto(
        @JsonProperty("min") double min,
        @JsonProperty("max") double max,
        @JsonProperty("mean") double mean,
        @JsonProperty("count") long count
) {}
