package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;
import java.util.Map;

public record SearchPageView(
        TrackingContext tracking,
        String searchTerm,
        List<ProductSummary> products) implements PixelEvent {

    @Override public String type()  { return "pageview"; }
    @Override public String ptype() { return "search"; }

    @Override
    public Map<String, String> typeParams() {
        return PixelEvent.notBlank(searchTerm) ? Map.of("search_term", searchTerm) : Map.of();
    }
}
