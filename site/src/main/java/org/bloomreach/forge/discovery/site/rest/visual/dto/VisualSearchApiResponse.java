package org.bloomreach.forge.discovery.site.rest.visual.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDoc;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualSearchApiResponse(@JsonProperty("response") Body response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonProperty("numFound") long numFound,
            @JsonProperty("docs") List<ProductDoc> docs) {}
}
