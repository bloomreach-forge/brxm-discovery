package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FacetValueDto(
        @JsonProperty("name") String name,
        @JsonProperty("count") long count,
        @JsonProperty("cat_id") String catId,
        @JsonProperty("cat_name") String catName,
        @JsonProperty("crumb") String crumb,
        @JsonProperty("tree_path") String treePath,
        @JsonProperty("parent") String parent,
        @JsonProperty("start") Double start,
        @JsonProperty("end") Double end
) {}
