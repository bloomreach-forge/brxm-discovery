package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryGridComponentInfo;
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
 * <p>Requires a linked {@code brxdis:categoryDocument}. The document dictates the
 * category ID mode:
 * <ul>
 *   <li><b>Pinned</b> - {@code brxdis:categoryId} is non-blank: use that ID directly.</li>
 *   <li><b>Dynamic</b> - {@code brxdis:categoryId} is blank: resolve from a URL path segment (e.g. {@code /cid/root-cat-id}) or query parameter (e.g. {@code ?cid=}); path segment takes priority.</li>
 * </ul>
 *
 * <p>If no document is configured the component renders nothing and shows a warning
 * in Experience Manager edit mode.
 *
 * <p>Catalog entry: {@code /product-grid-category} - "Discovery Product Grid - Category".
 */
@ParametersInfo(type = DiscoveryCategoryGridComponentInfo.class)
public class DiscoveryCategoryGridComponent extends AbstractDiscoveryGridComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCategoryGridComponent.class);
    @Override
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        DiscoveryCategoryGridComponentInfo info = getComponentParametersInfo(request);
        Map<String, String[]> params = getServletParameters(request);

        DiscoveryCategoryBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryCategoryBean.class);
        request.setModel(DiscoveryModelKeys.DOCUMENT, document);
        request.setModel(DiscoveryModelKeys.DATA_SOURCE_MODE, "category");

        if (document == null) {
            request.setModel(DiscoveryModelKeys.CATEGORY_ID, "");
            setEmptyState(request);
            return;
        }

        String categoryId = resolvePinnedOrDynamic(
                document.getCategoryId(), request, info.getCategoryUrlParam());
        request.setModel(DiscoveryModelKeys.CATEGORY_ID, categoryId != null ? categoryId : "");

        if (categoryId == null || categoryId.isBlank()) {
            if (isEditMode(request)) {
                String param = info.getCategoryUrlParam();
                request.setAttribute("brxdis_warning",
                        "Category document is in Dynamic mode but no category ID was found. " +
                        "Ensure the sitemap maps the URL segment to parameter '" + param +
                        "', or pass '?" + param + "=' as a query string.");
            }
            setEmptyState(request);
            return;
        }

        browseCategoryById(request, info, params, categoryId);
    }

    private void browseCategoryById(HstRequest request, DiscoveryCategoryGridComponentInfo info,
                                     Map<String, String[]> params, String categoryId) {
        SearchResponse browseResponse = getDiscoveryService().browse(request, categoryId, new SearchRequestOptions(
                info.getPageSize(),
                blankToNull(info.getDefaultSort()),
                blankToNull(info.getCatalogName()),
                parseStatsFields(info.getStatsFields()),
                blankToNull(info.getSegment()),
                blankToNull(info.getExclusionFilter())));

        request.setModel(DiscoveryModelKeys.DISPLAY_NAME, browseResponse.metadata().categoryName());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, browseResponse.metadata().campaign());
        request.setModel(DiscoveryModelKeys.STATS, browseResponse.metadata().stats());

        log.debug("Discovery category '{}' → {} results", categoryId, browseResponse.result().total());
        populateResultModels(request, browseResponse,
                info.isShowFacets(), info.isShowPagination(), info.isShowSort(), params,
                parseFacetFields(info.getFacetFields()));
    }
}
