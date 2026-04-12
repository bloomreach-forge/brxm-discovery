package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchGridComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoverySearchGridComponentInfo info = getComponentParametersInfo(request);
        Map<String, String[]> params = getServletParameters(request);

        String query = getPublicRequestParameter(request, "q");
        query = query != null ? query.trim() : "";

        request.setModel(DiscoveryModelKeys.QUERY, query);
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "search");

        if (query.isBlank()) {
            setEmptyState(request);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        SearchResponse searchResponse = svc.search(request, new SearchRequestOptions(
                info.getPageSize(), info.getDefaultSort(), blankToNull(info.getCatalogName()),
                parseStatsFields(info.getStatsFields()),
                info.getSegment(), info.getExclusionFilter()));

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
                info.isShowFacets(), info.isShowPagination(), info.isShowSort(), params);
    }
}
