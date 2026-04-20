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

import java.util.List;
import java.util.Map;

/**
 * Discovery Product Grid - Category browse mode.
 *
 * <p>Requires a linked {@code brxdis:categoryDocument}. The document dictates the
 * category ID mode:
 * <ul>
 *   <li><b>Pinned</b> - {@code brxdis:categoryId} is non-blank: use that ID directly.</li>
 *   <li><b>Dynamic</b> - {@code brxdis:categoryId} is blank: read the {@code ?category=} URL parameter.</li>
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

        // Document is required - no silent URL-param fallback without a document
        if (document == null) {
            request.setModel(DiscoveryModelKeys.CATEGORY_ID, "");
            setEmptyState(request);
            return;
        }

        // Document dictates mode: non-blank = Pinned, blank = Dynamic (URL param)
        String docCategoryId = document.getCategoryId();
        String categoryId;
        if (docCategoryId != null && !docCategoryId.isBlank()) {
            categoryId = docCategoryId;
        } else {
            // Path takes precedence: /category/{id} → sitemap param "1"
            String pathCategoryId = getPathSegmentParam(request, "1");
            categoryId = pathCategoryId != null ? pathCategoryId
                    : getPublicRequestParameter(request, info.getCategoryUrlParam());
        }

        request.setModel(DiscoveryModelKeys.CATEGORY_ID, categoryId != null ? categoryId : "");

        if (categoryId == null || categoryId.isBlank()) {
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                        "Category document is in Dynamic mode but no category ID was found in the URL path " +
                        "(/category/{id}) or '?" + info.getCategoryUrlParam() + "=' query parameter.");
            }
            setEmptyState(request);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        SearchResponse browseResponse = svc.browse(request, categoryId, new SearchRequestOptions(
                info.getPageSize(), blankToNull(info.getDefaultSort()), null, List.of(), null, null));

        request.setModel(DiscoveryModelKeys.DISPLAY_NAME, browseResponse.metadata().categoryName());
        request.setModel(DiscoveryModelKeys.CAMPAIGN, browseResponse.metadata().campaign());
        request.setModel(DiscoveryModelKeys.STATS, browseResponse.metadata().stats());

        log.debug("Discovery category '{}' → {} results", categoryId, browseResponse.result().total());
        populateResultModels(request, browseResponse,
                info.isShowFacets(), info.isShowPagination(), info.isShowSort(), params);
    }
}
