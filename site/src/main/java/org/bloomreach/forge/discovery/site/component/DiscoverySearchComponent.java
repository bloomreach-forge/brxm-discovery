package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.SearchResponse;

import java.io.IOException;
import java.util.List;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = DiscoverySearchComponentInfo.class)
public class DiscoverySearchComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoverySearchComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoverySearchComponentInfo info = getComponentParametersInfo(request);
        String label = info.getLabel();

        // Signal to view components (ProductGrid, Facets) that a search data source is wired
        // to this label — must happen before any early return so view components never show a
        // false "missing data source" warning just because no query has been typed yet.
        DiscoveryRequestCache.markSearchBandPresent(request, label);

        String query = getPublicRequestParameter(request, "q");
        query = (query != null) ? query.trim() : "";

        boolean suggestOnlyMode = "1".equals(getPublicRequestParameter(request, "brxdis_suggest"));

        // Config models — always set so FTL can render form/input correctly regardless of query
        request.setModel("label", label);
        request.setModel("suggestionsEnabled", info.isSuggestionsEnabled());
        request.setModel("resultsPage", info.getResultsPage());
        request.setModel("minChars", info.getMinChars());
        request.setModel("debounceMs", info.getDebounceMs());
        request.setModel("placeholder", info.getPlaceholder());
        request.setModel("query", query);
        request.setModel("suggestOnlyMode", suggestOnlyMode);

        if (query.isBlank()) {
            request.setModel("searchResult", null);
            request.setModel("autosuggestResult", null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();

        if (!suggestOnlyMode) {
            String catalogName = info.getCatalogName();
            List<String> statsFields = parseStatsFields(info.getStatsFields());
            SearchResponse searchResponse = svc.search(request, new SearchRequestOptions(
                    info.getPageSize(), info.getDefaultSort(), blankToNull(catalogName),
                    label, statsFields, info.getSegment(), info.getExclusionFilter()));
            request.setModel("searchResult", searchResponse.result());
            request.setModel("stats", searchResponse.metadata().stats());
            request.setModel("didYouMean", searchResponse.metadata().didYouMean());
            request.setModel("autoCorrectQuery", searchResponse.metadata().autoCorrectQuery());
            request.setModel("redirectUrl", searchResponse.metadata().redirectUrl());
            request.setModel("redirectQuery", searchResponse.metadata().redirectQuery());
            request.setModel("campaign", searchResponse.metadata().campaign());

            String redirectUrl = searchResponse.metadata().redirectUrl();
            if (info.isAutoRedirect() && redirectUrl != null && !redirectUrl.isBlank()) {
                try {
                    response.sendRedirect(redirectUrl);
                } catch (IOException e) {
                    log.warn("Keyword redirect to '{}' failed: {}", redirectUrl, e.getMessage());
                }
                return;
            }
            log.debug("Discovery search '{}' → {} results (page {})",
                    query, searchResponse.result().total(), searchResponse.result().page());
        }

        if (info.isSuggestionsEnabled()) {
            AutosuggestResult suggestions = svc.autosuggest(request, query, info.getSuggestionsLimit());
            request.setModel("autosuggestResult", suggestions);
        } else {
            request.setModel("autosuggestResult", null);
        }
    }

}

