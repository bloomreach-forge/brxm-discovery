package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;
import java.util.Map;

public record CategoryPageView(
        TrackingContext tracking,
        String categoryId,
        List<ProductSummary> products) implements PixelEvent {

    @Override public String type()  { return "pageview"; }
    @Override public String ptype() { return "category"; }

    @Override
    public Map<String, String> typeParams() {
        return PixelEvent.notBlank(categoryId) ? Map.of("cat_id", categoryId) : Map.of();
    }
}
