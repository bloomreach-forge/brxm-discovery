package org.bloomreach.forge.discovery.site.service.discovery.recommendation.model;

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
        String brUid2
) {

    /** Backwards-compatible constructor for v1 usage (no widgetType, fields, filters, url). */
    public RecQuery(String widgetId, String contextProductId, String contextPageType, int limit) {
        this(null, widgetId, contextProductId, contextPageType, limit, null, null, null, null, null);
    }
}
