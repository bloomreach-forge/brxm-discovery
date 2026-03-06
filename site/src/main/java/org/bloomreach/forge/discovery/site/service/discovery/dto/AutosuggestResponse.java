package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AutosuggestResponse(
        @JsonProperty("queryContext") QueryContext queryContext,
        @JsonProperty("suggestionGroups") List<SuggestionGroup> suggestionGroups
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QueryContext(
            @JsonProperty("originalQuery") String originalQuery
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SuggestionGroup(
            @JsonProperty("catalogName") String catalogName,
            @JsonProperty("view") String view,
            @JsonProperty("querySuggestions") List<QuerySuggestion> querySuggestions,
            @JsonProperty("attributeSuggestions") List<AttributeSuggestion> attributeSuggestions,
            @JsonProperty("searchSuggestions") List<ProductDoc> searchSuggestions
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QuerySuggestion(
            @JsonProperty("query") String query,
            @JsonProperty("displayText") String displayText
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AttributeSuggestion(
            @JsonProperty("name") String name,
            @JsonProperty("value") String value,
            @JsonProperty("attributeType") String attributeType
    ) {}
}
