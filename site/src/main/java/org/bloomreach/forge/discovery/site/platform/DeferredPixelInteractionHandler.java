package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DeferredPixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DiscoveryPixelService;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fires deferred pixel interaction events (search-submit, suggest-click, widget-click) based
 * on request parameters written by the front-end. Deduplicates events per request via an
 * {@link HstRequestContext} attribute to prevent double-firing within a single page render.
 *
 * <p>Actor: Analytics / Pixel team.
 */
final class DeferredPixelInteractionHandler {

    private static final String PROCESSED_INTERACTIONS_ATTR =
            DeferredPixelInteractionHandler.class.getName() + ".processedInteractions";

    private static final String EVENT_PARAM = "brxdis_event";
    private static final String EVENT_SEARCH_SUBMIT = "search-submit";
    private static final String EVENT_SUGGEST_CLICK = "suggest-click";
    private static final String EVENT_WIDGET_CLICK = "widget-click";
    private static final String AUTO_QUERY_PARAM = "brxdis_aq";
    private static final String WIDGET_ID_PARAM = "brxdis_wid";
    private static final String WIDGET_TYPE_PARAM = "brxdis_wty";
    private static final String WIDGET_RESULT_ID_PARAM = "brxdis_wrid";
    private static final String WIDGET_QUERY_PARAM = "brxdis_wq";

    private final DiscoveryPixelService pixelService;

    DeferredPixelInteractionHandler(DiscoveryPixelService pixelService) {
        this.pixelService = pixelService;
    }

    void handleSearchInteraction(HstRequest request, DiscoveryRuntimeContext runtimeContext,
                                 SearchQuery query) {
        String event = publicRequestParameter(request, EVENT_PARAM);
        if (event == null || event.isBlank()) {
            return;
        }
        if (EVENT_SEARCH_SUBMIT.equals(event)) {
            if (markInteractionProcessed(request, EVENT_SEARCH_SUBMIT + ":" + query.query())) {
                pixelService.fireDeferredEvent(DeferredPixelEvent.searchSubmit(
                                "search", runtimeContext.pageTitle(), runtimeContext.pageUrl(),
                                runtimeContext.refUrl(), runtimeContext.origRefUrl(),
                                runtimeContext.brUid2(), query.query()),
                        runtimeContext.credentials(), runtimeContext.clientIp(),
                        runtimeContext.clientContext(), runtimeContext.pixelFlags());
            }
            return;
        }
        if (EVENT_SUGGEST_CLICK.equals(event)) {
            String autoQuery = publicRequestParameter(request, AUTO_QUERY_PARAM);
            if (autoQuery == null || autoQuery.isBlank()) {
                return;
            }
            String eventKey = EVENT_SUGGEST_CLICK + ":" + autoQuery + ":" + query.query();
            if (markInteractionProcessed(request, eventKey)) {
                pixelService.fireDeferredEvent(DeferredPixelEvent.suggestClick(
                                "search", runtimeContext.pageTitle(), runtimeContext.pageUrl(),
                                runtimeContext.refUrl(), runtimeContext.origRefUrl(),
                                runtimeContext.brUid2(), autoQuery, query.query()),
                        runtimeContext.credentials(), runtimeContext.clientIp(),
                        runtimeContext.clientContext(), runtimeContext.pixelFlags());
            }
        }
    }

    void handleProductInteraction(HstRequest request, DiscoveryRuntimeContext runtimeContext,
                                  ProductSummary product) {
        String event = publicRequestParameter(request, EVENT_PARAM);
        if (!EVENT_WIDGET_CLICK.equals(event)) {
            return;
        }
        String widgetId = publicRequestParameter(request, WIDGET_ID_PARAM);
        String widgetType = publicRequestParameter(request, WIDGET_TYPE_PARAM);
        String widgetResultId = publicRequestParameter(request, WIDGET_RESULT_ID_PARAM);
        if (widgetId == null || widgetId.isBlank() || widgetType == null || widgetType.isBlank()
                || widgetResultId == null || widgetResultId.isBlank()) {
            return;
        }
        String widgetQuery = publicRequestParameter(request, WIDGET_QUERY_PARAM);
        String itemId = product != null ? product.id() : null;
        String eventKey = EVENT_WIDGET_CLICK + ":" + widgetId + ":" + widgetResultId + ":" + itemId;
        if (!markInteractionProcessed(request, eventKey)) {
            return;
        }
        pixelService.fireDeferredEvent(DeferredPixelEvent.widgetClick(
                        "product", runtimeContext.pageTitle(), runtimeContext.pageUrl(),
                        runtimeContext.refUrl(), runtimeContext.origRefUrl(),
                        runtimeContext.brUid2(), itemId, widgetId, widgetType, widgetResultId, widgetQuery),
                runtimeContext.credentials(), runtimeContext.clientIp(),
                runtimeContext.clientContext(), runtimeContext.pixelFlags());
    }

    @SuppressWarnings("unchecked")
    private boolean markInteractionProcessed(HstRequest request, String key) {
        Object attr = request.getRequestContext().getAttribute(PROCESSED_INTERACTIONS_ATTR);
        Set<String> processed;
        if (attr instanceof Set<?>) {
            processed = (Set<String>) attr;
        } else {
            processed = ConcurrentHashMap.newKeySet();
            request.getRequestContext().setAttribute(PROCESSED_INTERACTIONS_ATTR, processed);
        }
        return processed.add(key);
    }

    private static String publicRequestParameter(HstRequest request, String name) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext == null) {
            return null;
        }
        HttpServletRequest servletRequest = requestContext.getServletRequest();
        return servletRequest != null ? servletRequest.getParameter(name) : null;
    }
}
