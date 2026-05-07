package org.bloomreach.forge.discovery.site.rest.visual.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisualUploadApiResponse(@JsonProperty("response") Body response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            @JsonProperty("image_id") String imageId,
            @JsonProperty("objects") List<ObjectDoc> objects) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ObjectDoc(
            @JsonProperty("id") int id,
            @JsonProperty("bbox") double[] bbox,
            @JsonProperty("object_type") String objectType) {}
}
