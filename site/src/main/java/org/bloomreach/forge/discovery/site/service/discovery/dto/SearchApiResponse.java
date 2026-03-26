package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchApiResponse(
        @JsonProperty("response") ApiResponseBody response,
        @JsonProperty("facet_counts") FacetCounts facetCounts,
        @JsonProperty("stats") StatsDto stats,
        @JsonProperty("did_you_mean") List<String> didYouMean,
        @JsonProperty("autoCorrectQuery") String autoCorrectQuery,
        @JsonProperty("keywordRedirect") KeywordRedirectDto keywordRedirect,
        @JsonProperty("campaign") CampaignDto campaign,
        @JsonProperty("category_map") Map<String, String> categoryMap
) {}
