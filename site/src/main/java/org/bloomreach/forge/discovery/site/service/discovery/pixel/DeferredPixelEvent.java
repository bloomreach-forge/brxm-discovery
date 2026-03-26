package org.bloomreach.forge.discovery.site.service.discovery.pixel;

public record DeferredPixelEvent(
        String group,
        String etype,
        String ptype,
        String title,
        String url,
        String refUrl,
        String origRefUrl,
        String brUid2,
        String query,
        String autoQuery,
        String widgetId,
        String widgetType,
        String widgetResultId,
        String widgetQuery,
        String itemId
) {

    public static DeferredPixelEvent searchSubmit(String pageType, String title, String url, String refUrl,
                                                  String origRefUrl, String brUid2, String query) {
        return new DeferredPixelEvent("suggest", "submit", pageType, title, url, refUrl, origRefUrl,
                brUid2, query, null, null, null, null, null, null);
    }

    public static DeferredPixelEvent suggestClick(String pageType, String title, String url, String refUrl,
                                                  String origRefUrl, String brUid2, String autoQuery, String query) {
        return new DeferredPixelEvent("suggest", "click", pageType, title, url, refUrl, origRefUrl,
                brUid2, query, autoQuery, null, null, null, null, null);
    }

    public static DeferredPixelEvent widgetClick(String pageType, String title, String url, String refUrl,
                                                 String origRefUrl, String brUid2, String itemId,
                                                 String widgetId, String widgetType,
                                                 String widgetResultId, String widgetQuery) {
        return new DeferredPixelEvent("widget", "click", pageType, title, url, refUrl, origRefUrl,
                brUid2, null, null, widgetId, widgetType, widgetResultId, widgetQuery, itemId);
    }
}
