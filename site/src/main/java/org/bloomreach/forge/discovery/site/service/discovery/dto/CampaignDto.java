package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CampaignDto(
        @JsonProperty("id") String id,
        @JsonProperty("campaignName") String campaignName,
        @JsonProperty("htmlText") String htmlText,
        @JsonProperty("bannerUrl") String bannerUrl,
        @JsonProperty("imageUrl") String imageUrl
) {}
