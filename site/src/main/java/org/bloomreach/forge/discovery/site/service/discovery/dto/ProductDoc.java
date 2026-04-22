package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a single product document from the Discovery API response.
 * The five named fields are the Discovery protocol essentials present in
 * virtually every feed. All other feed-specific fields (brand, description,
 * pet_type, review_count, etc.) flow through {@link #extras()}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ProductDoc {

    @JsonProperty("pid")         private String pid;
    @JsonProperty("title")       private String title;
    @JsonProperty("url")         private String url;
    @JsonProperty("thumb_image") private String thumbImage;
    @JsonProperty("price")       private BigDecimal price;
    @JsonProperty("currency")    private String currency;

    private final Map<String, Object> extras = new HashMap<>();

    @JsonAnySetter
    public void set(String key, Object value) {
        if (value != null) {
            extras.put(key, value);
        }
    }

    public String pid()         { return pid; }
    public String title()       { return title; }
    public String url()         { return url; }
    public String thumbImage()  { return thumbImage; }
    public BigDecimal price()   { return price; }
    public String currency()    { return currency; }

    public Map<String, Object> extras() { return Map.copyOf(extras); }

    public Optional<Object> get(String key) { return Optional.ofNullable(extras.get(key)); }
}
