package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDoc(
        @JsonProperty("pid") String pid,
        @JsonProperty("title") String title,
        @JsonProperty("url") String url,
        @JsonProperty("thumb_image") String thumbImage,
        @JsonProperty("price") BigDecimal price,
        @JsonProperty("sale_price") BigDecimal salePrice,
        @JsonProperty("currency") String currency,
        @JsonProperty("brand") String brand,
        @JsonProperty("description") String description
) {}
