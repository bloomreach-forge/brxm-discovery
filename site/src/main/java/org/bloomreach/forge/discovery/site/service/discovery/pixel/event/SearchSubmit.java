package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import java.util.Map;

public record SearchSubmit(
        TrackingContext tracking,
        String query,
        String ptype) implements PixelEvent {

    @Override public String type()  { return "event"; }
    @Override public String group() { return "suggest"; }
    @Override public String etype() { return "submit"; }
    @Override public String ptype() { return PixelEvent.notBlank(ptype) ? ptype : "search"; }

    @Override
    public Map<String, String> typeParams() {
        return PixelEvent.notBlank(query) ? Map.of("q", query) : Map.of();
    }
}
