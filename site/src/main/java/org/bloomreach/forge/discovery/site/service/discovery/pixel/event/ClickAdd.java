package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record ClickAdd(
        TrackingContext tracking,
        String itemId,
        String sku,
        String ptype) implements PixelEvent {

    @Override public String type()  { return "event"; }
    @Override public String group() { return "cart"; }
    @Override public String etype() { return "click-add"; }
    @Override public String ptype() { return PixelEvent.notBlank(ptype) ? ptype : "product"; }

    @Override
    public Map<String, String> typeParams() {
        Map<String, String> p = new LinkedHashMap<>();
        if (PixelEvent.notBlank(itemId)) p.put("item_id", itemId);
        if (PixelEvent.notBlank(sku))    p.put("sku",     sku);
        return p;
    }
}
