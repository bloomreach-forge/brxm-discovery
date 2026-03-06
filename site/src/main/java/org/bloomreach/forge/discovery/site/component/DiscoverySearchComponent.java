package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoverySearchComponentInfo;
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

        String searchTerm = getPublicRequestParameter(request, "q");
        if (searchTerm != null) {
            searchTerm = searchTerm.trim();
        }

        if (searchTerm == null || searchTerm.isBlank()) {
            request.setModel("query", "");
            request.setModel("searchResult", null);
            request.setAttribute("query", "");
            return;
        }

        HstDiscoveryService svc = lookupService(HstDiscoveryService.class);
        String catalogName = info.getCatalogName();
        SearchResult result = (catalogName != null && !catalogName.isBlank())
                ? svc.search(request, info.getPageSize(), info.getDefaultSort(), catalogName)
                : svc.search(request, info.getPageSize(), info.getDefaultSort());

        request.setModel("query", searchTerm);
        request.setModel("searchResult", result);
        request.setAttribute("query", searchTerm);
        request.setAttribute("searchResult", result);

        log.debug("Discovery search '{}' → {} results (page {})",
                searchTerm, result.total(), result.page());
    }
}
