package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
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
    static final String CAT_ID_PARAM = "category";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryComponentInfo info = getComponentParametersInfo(request);
        String label = info.getLabel();

        // Signal to view components (ProductGrid, Facets) that a category data source is wired
        // to this label — must happen before any early return so view components never show a
        // false "missing data source" warning just because no category is configured yet.
        DiscoveryRequestCache.markCategoryBandPresent(request, label);

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
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                        "No category configured. Attach a Category Document to this component " +
                        "or pass a '?category=' URL parameter.");
            }
            setModelAndAttribute(request, "categoryId", "");
            setModelAndAttribute(request, "categoryResult", emptyResult());
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        List<String> statsFields = parseStatsFields(info.getStatsFields());
        SearchResponse browseResponse = svc.browse(request, categoryId, info.getPageSize(), info.getDefaultSort(),
                label, statsFields, info.getSegment(), info.getExclusionFilter());

        setModelAndAttribute(request, "categoryId", categoryId);
        setModelAndAttribute(request, "categoryResult", browseResponse.result());
        setModelAndAttribute(request, "stats", browseResponse.metadata().stats());
        setModelAndAttribute(request, "label", label);
        setModelAndAttribute(request, "campaign", browseResponse.metadata().campaign());

        log.debug("Category '{}' returned {} results (page {})",
                categoryId, browseResponse.result().total(), browseResponse.result().page());
    }

    private static SearchResult emptyResult() {
        return new SearchResult(List.of(), 0L, 0, 0, Map.of());
    }

}

