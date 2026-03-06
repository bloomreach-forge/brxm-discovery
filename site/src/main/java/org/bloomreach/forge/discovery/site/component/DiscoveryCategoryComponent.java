package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@ParametersInfo(type = DiscoveryCategoryComponentInfo.class)
public class DiscoveryCategoryComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCategoryComponent.class);
    static final String CAT_ID_PARAM = "categoryId";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryComponentInfo info = getComponentParametersInfo(request);

        // 1. Category document (editor-configured via Channel Manager)
        DiscoveryCategoryBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryCategoryBean.class);
        request.setAttribute("document", document);
        String categoryId = document != null && document.getCategoryId() != null
                && !document.getCategoryId().isBlank()
                ? document.getCategoryId()
                : getPublicRequestParameter(request, CAT_ID_PARAM);

        // 2. Null guard — nothing configured; return empty to avoid invalid API call
        if (categoryId == null || categoryId.isBlank()) {
            boolean cmsRequest = request.getRequestContext().isChannelManagerPreviewRequest();
            if (cmsRequest) {
                request.setAttribute("brxdis_warning",
                        "No category configured. Attach a Category Document to this component " +
                        "or pass a '?categoryId=' URL parameter.");
            }
            request.setModel("categoryId", "");
            request.setModel("categoryResult", emptyResult());
            request.setAttribute("categoryId", "");
            request.setAttribute("categoryResult", emptyResult());
            return;
        }

        HstDiscoveryService svc = lookupService(HstDiscoveryService.class);
        SearchResult result = svc.browse(request, categoryId, info.getPageSize(), info.getDefaultSort());

        request.setModel("categoryId", categoryId);
        request.setModel("categoryResult", result);
        request.setAttribute("categoryId", categoryId);
        request.setAttribute("categoryResult", result);

        log.debug("Category '{}' returned {} results (page {})",
                categoryId, result.total(), result.page());
    }

    private static SearchResult emptyResult() {
        return new SearchResult(List.of(), 0L, 0, 0, Map.of());
    }
}
