package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;

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
        String band = info.getBandName();

        // Signal to view components (ProductGrid, Facets) that a search data source is wired
        // to this band — must happen before any early return so view components never show a
        // false "missing data source" warning just because no query has been typed yet.
        DiscoveryRequestCache.markSearchBandPresent(request, band);

        String query = getPublicRequestParameter(request, "q");
        query = (query != null) ? query.trim() : "";

        boolean suggestOnlyMode = "1".equals(getPublicRequestParameter(request, "brxdis_suggest"));

        // Config models — always set so FTL can render form/input correctly regardless of query
        setModelAndAttribute(request, "dataBand", band);
        setModelAndAttribute(request, "suggestionsEnabled", info.isSuggestionsEnabled());
        setModelAndAttribute(request, "resultsPage", info.getResultsPage());
        setModelAndAttribute(request, "minChars", info.getMinChars());
        setModelAndAttribute(request, "debounceMs", info.getDebounceMs());
        setModelAndAttribute(request, "placeholder", info.getPlaceholder());
        setModelAndAttribute(request, "query", query);
        setModelAndAttribute(request, "suggestOnlyMode", suggestOnlyMode);

        if (query.isBlank()) {
            request.setModel("searchResult", null);
            setModelAndAttribute(request, "autosuggestResult", null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();

        if (!suggestOnlyMode) {
            String catalogName = info.getCatalogName();
            SearchResult result = svc.search(request, info.getPageSize(), info.getDefaultSort(),
                    (catalogName != null && !catalogName.isBlank()) ? catalogName : null, band);
            setModelAndAttribute(request, "searchResult", result);
            log.debug("Discovery search '{}' → {} results (page {})", query, result.total(), result.page());
        }

        if (info.isSuggestionsEnabled()) {
            AutosuggestResult suggestions = svc.autosuggest(request, query, info.getSuggestionsLimit());
            setModelAndAttribute(request, "autosuggestResult", suggestions);
        } else {
            setModelAndAttribute(request, "autosuggestResult", null);
        }
    }
}
