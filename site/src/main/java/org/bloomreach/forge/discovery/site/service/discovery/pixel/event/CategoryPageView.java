package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;
import java.util.Map;

public record CategoryPageView(
        TrackingContext tracking,
        String categoryId,
        String categoryName,
        List<ProductSummary> products) implements PixelEvent {

    @Override public String type()  { return "pageview"; }
    @Override public String ptype() { return "category"; }

    @Override
    public Map<String, String> typeParams() {
        var params = new java.util.LinkedHashMap<String, String>();
        if (PixelEvent.notBlank(categoryId))   params.put("cat_id", categoryId);
        if (PixelEvent.notBlank(categoryName)) params.put("cat", categoryName);
        return java.util.Collections.unmodifiableMap(params);
    }
}
