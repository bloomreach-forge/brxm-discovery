package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecommendationResponse(
        @JsonProperty("response") ApiResponseBody response,
        @JsonProperty("metadata") WidgetMetadata metadata
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WidgetMetadata(@JsonProperty("widget") WidgetInfo widget) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WidgetInfo(@JsonProperty("rid") String rid) {}
}
