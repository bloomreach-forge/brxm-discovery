package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record SuggestClick(
        TrackingContext tracking,
        String query,
        String autoQuery,
        String ptype) implements PixelEvent {

    @Override public String type()  { return "event"; }
    @Override public String group() { return "suggest"; }
    @Override public String etype() { return "click"; }
    @Override public String ptype() { return PixelEvent.notBlank(ptype) ? ptype : "search"; }

    @Override
    public Map<String, String> typeParams() {
        Map<String, String> p = new LinkedHashMap<>();
        if (PixelEvent.notBlank(query))     p.put("q",  query);
        if (PixelEvent.notBlank(autoQuery)) p.put("aq", autoQuery);
        return p;
    }
}
