package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record ProductPageView(
        TrackingContext tracking,
        String pid,
        String prodName) implements PixelEvent {

    @Override public String type()              { return "pageview"; }
    @Override public String ptype()             { return "product"; }
    @Override public boolean keepSuggestQuery() { return true; }

    @Override
    public Map<String, String> typeParams() {
        Map<String, String> p = new LinkedHashMap<>();
        p.put("prod_id", pid);
        if (PixelEvent.notBlank(prodName)) p.put("prod_name", prodName);
        return p;
    }
}
