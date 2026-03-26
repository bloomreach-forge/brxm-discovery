package org.bloomreach.forge.discovery.recommendation.model;

public record RecQuery(
        String widgetType,
        String widgetId,
        String contextProductId,
        String contextPageType,
        int limit,
        String fields,
        String filters,
        String url,
        String refUrl,
        String brUid2,
        String origRefUrl
) {

    /** Backwards-compatible constructor for v1 usage (no widgetType, fields, filters, url). */
    public RecQuery(String widgetId, String contextProductId, String contextPageType, int limit) {
        this(null, widgetId, contextProductId, contextPageType, limit, null, null, null, null, null, null);
    }

    /** Backwards-compatible constructor (pre-origRefUrl canonical signature). */
    public RecQuery(String widgetType, String widgetId, String contextProductId, String contextPageType,
                    int limit, String fields, String filters, String url, String refUrl, String brUid2) {
        this(widgetType, widgetId, contextProductId, contextPageType, limit, fields, filters, url, refUrl, brUid2, null);
    }
}
