package org.bloomreach.forge.discovery.site.component;

import org.hippoecm.hst.core.component.HstRequest;

/**
 * Base class for the three recommendation component variants (product, category, global).
 * Holds shared URL-parameter names and the widgetId resolution strategy
 * (document field wins over URL param {@code ?widgetId=}).
 */
abstract class AbstractDiscoveryRecommendationComponent extends AbstractDiscoveryComponent {

    static final String WIDGET_ID_PARAM = "widgetId";
    static final String LIMIT_PARAM     = "limit";
    static final String FIELDS_PARAM    = "fields";
    static final String FILTER_PARAM    = "filter";

    /**
     * Resolves the effective widget ID. The recommendation document's {@code widgetId}
     * property takes precedence; falls back to the {@code ?widgetId=} URL parameter.
     *
     * @param widgetIdFromDoc the widget ID read from the linked recommendation document,
     *                        or {@code null} if no document is linked or has no ID set
     */
    protected String resolveWidgetId(String widgetIdFromDoc, HstRequest request) {
        return (widgetIdFromDoc != null && !widgetIdFromDoc.isBlank())
                ? widgetIdFromDoc
                : getPublicRequestParameter(request, WIDGET_ID_PARAM);
    }
}
