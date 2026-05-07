package org.bloomreach.forge.discovery.site.service.discovery.pixel.event;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record WidgetView(
        TrackingContext tracking,
        String widgetId,
        String widgetType,
        String widgetResultId,
        String widgetQuery,
        String ptype,
        List<ProductSummary> products) implements PixelEvent {

    @Override public String type()   { return "event"; }
    @Override public String group()  { return "widget"; }
    @Override public String etype()  { return "widget-view"; }
    @Override public String ptype()  { return PixelEvent.notBlank(ptype) ? ptype : "content"; }

    @Override
    public Map<String, String> typeParams() {
        Map<String, String> p = new LinkedHashMap<>();
        if (PixelEvent.notBlank(widgetId))       p.put("wid",  widgetId);
        if (PixelEvent.notBlank(widgetType))     p.put("wty",  widgetType);
        if (PixelEvent.notBlank(widgetResultId)) p.put("wrid", widgetResultId);
        if (PixelEvent.notBlank(widgetQuery))    p.put("wq",   widgetQuery);
        return p;
    }
}
