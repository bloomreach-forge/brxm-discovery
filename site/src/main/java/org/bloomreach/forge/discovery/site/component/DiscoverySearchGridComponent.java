package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchGridComponentInfo;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Discovery Product Grid - Search mode.
 *
 * <p>Executes a keyword search against the Discovery API and populates the page model
 * with products, facets, pagination, sort URLs, and search metadata (did-you-mean,
 * auto-correct, keyword redirects). Renders via {@code brxdis-results.ftl}.
 *
 * <p>Catalog entry: {@code /product-grid-search} - "Discovery Product Grid - Search".
 */
@ParametersInfo(type = DiscoverySearchGridComponentInfo.class)
public class DiscoverySearchGridComponent extends AbstractDiscoveryGridComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoverySearchGridComponent.class);

    private record VisualSearchContext(boolean enabled, String widgetId, String imageId) {
        boolean isImageSearchRequest() {
            return widgetId != null && imageId != null && !imageId.isBlank();
        }
    }

    @Override
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        DiscoverySearchGridComponentInfo info = getComponentParametersInfo(request);
        Map<String, String[]> params = getServletParameters(request);

        String query = getPublicRequestParameter(request, "q");
        query = query != null ? query.trim() : "";
        request.setModel(DiscoveryModelKeys.QUERY, query);

        VisualSearchContext vsCtx = buildVisualSearchContext(request);
        applyVisualSearchModels(request, vsCtx);

        if (vsCtx.isImageSearchRequest()) {
            searchByImage(request, info, vsCtx.widgetId(), vsCtx.imageId());
        } else {
            request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "search");
            if (query.isBlank()) {
                setEmptyState(request);
                return;
            }
            searchByQuery(request, response, info, params, query);
        }
    }

    private VisualSearchContext buildVisualSearchContext(HstRequest request) {
        DiscoveryChannelInfo channelInfo = getChannelInfo(request);
        boolean enabled = channelInfo != null && channelInfo.getDiscoveryVisualSearchEnabled();
        if (!enabled) {
            return new VisualSearchContext(false, null, null);
        }
        String widgetId = resolveVisualSearchWidgetId(request, channelInfo);
        if (widgetId == null) {
            log.warn("Visual search enabled but no widgetId resolved; falling back to keyword search");
        }
        String imageId = widgetId != null ? getPublicRequestParameter(request, "imageId") : null;
        return new VisualSearchContext(enabled, widgetId, imageId);
    }

    private void applyVisualSearchModels(HstRequest request, VisualSearchContext vsCtx) {
        request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_ENABLED, vsCtx.enabled());
        if (vsCtx.widgetId() == null) {
            return;
        }
        String vsBase = request.getContextPath() + "/_brxdis-api/visual-search/" + vsCtx.widgetId();
        request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_UPLOAD_URL, vsBase + "/upload");
        request.setModel(DiscoveryModelKeys.VISUAL_SEARCH_WIDGET_ID, vsCtx.widgetId());
    }

    private void searchByImage(HstRequest request, DiscoverySearchGridComponentInfo info,
                                String widgetId, String imageId) {
        String objectId = getPublicRequestParameter(request, "objectId");
        List<ProductSummary> products = getDiscoveryService()
                .visualSearch(request, widgetId, imageId, objectId, info.getPageSize());
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "visual-search");
        request.setModel(DiscoveryModelKeys.PRODUCTS, products);
    }

    private void searchByQuery(HstRequest request, HstResponse response,
                                DiscoverySearchGridComponentInfo info,
                                Map<String, String[]> params, String query) {
        SearchResponse searchResponse = getDiscoveryService().search(request, new SearchRequestOptions(
                info.getPageSize(),
                blankToNull(info.getDefaultSort()),
                blankToNull(info.getCatalogName()),
                parseStatsFields(info.getStatsFields()),
                blankToNull(info.getSegment()),
                blankToNull(info.getExclusionFilter())));

        if (info.isShowDidYouMean()) {
            request.setModel(DiscoveryModelKeys.DID_YOU_MEAN, searchResponse.metadata().didYouMean());
        }
        request.setModel(DiscoveryModelKeys.AUTO_CORRECT_QUERY, searchResponse.metadata().autoCorrectQuery());
        request.setModel(DiscoveryModelKeys.REDIRECT_URL, searchResponse.metadata().redirectUrl());
        request.setModel(DiscoveryModelKeys.REDIRECT_QUERY, searchResponse.metadata().redirectQuery());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, searchResponse.metadata().campaign());

        String redirectUrl = searchResponse.metadata().redirectUrl();
        if (info.isAutoRedirect() && redirectUrl != null && !redirectUrl.isBlank()) {
            try {
                response.sendRedirect(redirectUrl);
            } catch (IOException e) {
                log.warn("Keyword redirect to '{}' failed: {}", redirectUrl, e.getMessage());
            }
            return;
        }

        log.debug("Discovery search '{}' → {} results", query, searchResponse.result().total());
        populateResultModels(request, searchResponse,
                info.isShowFacets(), info.isShowPagination(), info.isShowSort(), params,
                parseFacetFields(info.getFacetFields()));
    }
}
