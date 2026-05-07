package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import java.util.LinkedHashMap;
import java.util.Map;

public record WidgetClick(
        TrackingContext tracking,
        String widgetId,
        String widgetType,
        String widgetResultId,
        String widgetQuery,
        String itemId,
        String ptype) implements PixelEvent {

    @Override public String type()  { return "event"; }
    @Override public String group() { return "widget"; }
    @Override public String etype() { return "widget-click"; }
    @Override public String ptype() { return PixelEvent.notBlank(ptype) ? ptype : "content"; }

    @Override
    public Map<String, String> typeParams() {
        Map<String, String> p = new LinkedHashMap<>();
        if (PixelEvent.notBlank(widgetId))       p.put("wid",  widgetId);
        if (PixelEvent.notBlank(widgetType))     p.put("wty",  widgetType);
        if (PixelEvent.notBlank(widgetResultId)) p.put("wrid", widgetResultId);
        if (PixelEvent.notBlank(widgetQuery))    p.put("wq",   widgetQuery);
        if (PixelEvent.notBlank(itemId))         p.put("item_id", itemId);
        return p;
    }
}
