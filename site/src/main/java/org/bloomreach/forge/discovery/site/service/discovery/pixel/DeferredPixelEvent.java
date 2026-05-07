package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SearchSubmit;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.SuggestClick;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.TrackingContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.WidgetClick;

public final class DeferredPixelEvent {

    private DeferredPixelEvent() {}

    public static SearchSubmit searchSubmit(String pageType, String title, String url, String refUrl,
                                            String origRefUrl, String brUid2, String query) {
        return new SearchSubmit(new TrackingContext(brUid2, refUrl, origRefUrl, url, title), query, pageType);
    }

    public static SuggestClick suggestClick(String pageType, String title, String url, String refUrl,
                                            String origRefUrl, String brUid2, String autoQuery, String query) {
        return new SuggestClick(new TrackingContext(brUid2, refUrl, origRefUrl, url, title), query, autoQuery, pageType);
    }

    public static WidgetClick widgetClick(String pageType, String title, String url, String refUrl,
                                          String origRefUrl, String brUid2, String itemId,
                                          String widgetId, String widgetType,
                                          String widgetResultId, String widgetQuery) {
        return new WidgetClick(new TrackingContext(brUid2, refUrl, origRefUrl, url, title),
                widgetId, widgetType, widgetResultId, widgetQuery, itemId, pageType);
    }
}
