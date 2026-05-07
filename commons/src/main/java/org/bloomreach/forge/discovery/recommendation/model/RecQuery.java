package org.bloomreach.forge.discovery.recommendation.model;

public record RecQuery(
        String widgetType,
        String widgetId,
        String contextProductId,
        String catId,
        String contextPageType,
        int limit,
        String fields,
        String filters,
        String url,
        String refUrl,
        String brUid2,
        String origRefUrl,
        String query,
        String viewId
) {

    /** Backwards-compatible constructor for v1 usage (no widgetType, fields, filters, url). */
    public RecQuery(String widgetId, String contextProductId, String contextPageType, int limit) {
        this(null, widgetId, contextProductId, null, contextPageType, limit, null, null, null, null, null, null, null, null);
    }

    /** Backwards-compatible constructor (pre-catId, pre-origRefUrl canonical signature). */
    public RecQuery(String widgetType, String widgetId, String contextProductId, String contextPageType,
                    int limit, String fields, String filters, String url, String refUrl, String brUid2) {
        this(widgetType, widgetId, contextProductId, null, contextPageType, limit, fields, filters, url, refUrl, brUid2, null, null, null);
    }

    public RecQuery withViewId(String viewId) {
        return new RecQuery(widgetType, widgetId, contextProductId, catId, contextPageType, limit,
                fields, filters, url, refUrl, brUid2, origRefUrl, query, viewId);
    }
}
