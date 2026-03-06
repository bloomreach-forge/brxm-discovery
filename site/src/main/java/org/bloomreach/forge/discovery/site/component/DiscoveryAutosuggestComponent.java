package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryAutosuggestComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;

import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ParametersInfo(type = DiscoveryAutosuggestComponentInfo.class)
public class DiscoveryAutosuggestComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryAutosuggestComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryAutosuggestComponentInfo info = getComponentParametersInfo(request);

        String query = getPublicRequestParameter(request, "q");
        if (query != null) {
            query = query.trim();
        }

        if (query == null || query.isBlank()) {
            setModelAndAttribute(request, "query", "");
            request.setModel("autosuggestResult", null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        String catalogViews = info.getCatalogViews();
        AutosuggestResult result = (catalogViews != null && !catalogViews.isBlank())
                ? svc.autosuggest(request, query, info.getLimit(), catalogViews)
                : svc.autosuggest(request, query, info.getLimit());

        setModelAndAttribute(request, "query", query);
        setModelAndAttribute(request, "autosuggestResult", result);

        log.debug("Discovery autosuggest '{}' → {} query suggestions, {} product suggestions",
                query, result.querySuggestions().size(), result.productSuggestions().size());
    }
}
