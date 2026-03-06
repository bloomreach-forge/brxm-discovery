package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ApiResponseBody(
        @JsonProperty("numFound") long numFound,
        @JsonProperty("docs") List<ProductDoc> docs
) {}
