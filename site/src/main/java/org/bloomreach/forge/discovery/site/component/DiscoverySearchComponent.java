package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
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

        setModelAndAttribute(request, "dataBand", band);

        String searchTerm = getPublicRequestParameter(request, "q");
        if (searchTerm != null) {
            searchTerm = searchTerm.trim();
        }

        if (searchTerm == null || searchTerm.isBlank()) {
            setModelAndAttribute(request, "query", "");
            request.setModel("searchResult", null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        String catalogName = info.getCatalogName();
        SearchResult result = svc.search(request, info.getPageSize(), info.getDefaultSort(),
                (catalogName != null && !catalogName.isBlank()) ? catalogName : null, band);

        setModelAndAttribute(request, "query", searchTerm);
        setModelAndAttribute(request, "searchResult", result);

        log.debug("Discovery search '{}' → {} results (page {})",
                searchTerm, result.total(), result.page());
    }
}
