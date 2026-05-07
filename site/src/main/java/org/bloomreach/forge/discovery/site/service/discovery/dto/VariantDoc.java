package org.bloomreach.forge.discovery.site.service.discovery.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class VariantDoc {

    @JsonProperty("skuid")             private String skuId;
    @JsonProperty("sku_color")         private String skuColor;
    @JsonProperty("sku_color_group")   private String skuColorGroup;
    @JsonProperty("sku_size")          private String skuSize;
    @JsonProperty("sku_price")         private BigDecimal skuPrice;
    @JsonProperty("sku_sale_price")    private BigDecimal skuSalePrice;
    @JsonProperty("sku_thumb_images")  private List<String> skuThumbImages;
    @JsonProperty("sku_large_images")  private List<String> skuLargeImages;
    @JsonProperty("sku_swatch_images") private List<String> skuSwatchImages;

    private final Map<String, Object> extras = new HashMap<>();

    @JsonAnySetter
    public void set(String key, Object value) {
        if (value != null) extras.put(key, value);
    }

    public String skuId()                  { return skuId; }
    public String skuColor()               { return skuColor; }
    public String skuColorGroup()          { return skuColorGroup; }
    public String skuSize()                { return skuSize; }
    public BigDecimal skuPrice()           { return skuPrice; }
    public BigDecimal skuSalePrice()       { return skuSalePrice; }
    public List<String> skuThumbImages()   { return skuThumbImages; }
    public List<String> skuLargeImages()   { return skuLargeImages; }
    public List<String> skuSwatchImages()  { return skuSwatchImages; }
    public Map<String, Object> extras()    { return Map.copyOf(extras); }
}
