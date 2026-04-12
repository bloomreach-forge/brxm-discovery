package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryGridComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Discovery Product Grid - Category browse mode.
 *
 * <p>Resolves a category ID from either a linked Category Document or the
 * {@code ?category=} URL parameter, then fetches matching products from the
 * Discovery API. Renders via {@code brxdis-results.ftl}.
 *
 * <p>Catalog entry: {@code /product-grid-category} - "Discovery Product Grid - Category".
 */
@ParametersInfo(type = DiscoveryCategoryGridComponentInfo.class)
public class DiscoveryCategoryGridComponent extends AbstractDiscoveryGridComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCategoryGridComponent.class);
    static final String CAT_ID_PARAM = "category";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryGridComponentInfo info = getComponentParametersInfo(request);
        Map<String, String[]> params = getServletParameters(request);

        DiscoveryCategoryBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryCategoryBean.class);

        String categoryId = document != null
                && document.getCategoryId() != null
                && !document.getCategoryId().isBlank()
                ? document.getCategoryId()
                : getPublicRequestParameter(request, CAT_ID_PARAM);

        request.setModel(DiscoveryModelKeys.CATEGORY_ID, categoryId != null ? categoryId : "");
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "category");

        if (categoryId == null || categoryId.isBlank()) {
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                        "No category configured. Attach a Category Document to this component " +
                        "or pass a '?category=' URL parameter.");
            }
            setEmptyState(request);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        SearchResponse browseResponse = svc.browse(request, categoryId, new SearchRequestOptions(
                info.getPageSize(), info.getDefaultSort(), null,
                parseStatsFields(info.getStatsFields()),
                info.getSegment(), info.getExclusionFilter()));

        request.setModel(DiscoveryModelKeys.DISPLAY_NAME, browseResponse.metadata().categoryName());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, browseResponse.metadata().campaign());
        request.setModel(DiscoveryModelKeys.STATS, browseResponse.metadata().stats());

        log.debug("Discovery category '{}' → {} results", categoryId, browseResponse.result().total());
        populateResultModels(request, browseResponse,
                info.isShowFacets(), info.isShowPagination(), info.isShowSort(), params);
    }
}
