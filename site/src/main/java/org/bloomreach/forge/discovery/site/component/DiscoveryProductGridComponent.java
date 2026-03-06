package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryDataSourceComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import java.util.List;
import java.util.Optional;

/**
 * View component that reads products from a parent data-fetching component's cached result.
 * Configurable via component parameter {@code dataSource}: {@code "search"} (default) or {@code "category"}.
 */
@ParametersInfo(type = DiscoveryDataSourceComponentInfo.class)
public class DiscoveryProductGridComponent extends AbstractDiscoveryComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryDataSourceComponentInfo info = getComponentParametersInfo(request);
        boolean isCategory = "category".equals(info.getDataSource());
        Optional<SearchResult> result = isCategory
                ? DiscoveryRequestCache.getCategoryResult(request)
                : DiscoveryRequestCache.getSearchResult(request);

        warnIfMissingDataSource(request, result.isEmpty(), isCategory);

        List<?> products = result.map(SearchResult::products).orElse(List.of());
        request.setModel("products", products);
        request.setAttribute("products", products);

        PaginationModel pagination = result
                .map(r -> new PaginationModel(r.total(), r.page(), r.pageSize()))
                .orElse(new PaginationModel(0L, 0, 0));
        request.setModel("pagination", pagination);
        request.setAttribute("pagination", pagination);
    }
}
