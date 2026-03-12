package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryDataSourceComponentInfo;
import org.bloomreach.forge.discovery.search.model.PaginationModel;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import java.util.List;

/**
 * View component that reads products from a parent data-fetching component's cached result.
 * Auto-detects whether the producing component was Search or Category by probing both markers.
 */
@ParametersInfo(type = DiscoveryDataSourceComponentInfo.class)
public class DiscoveryProductGridComponent extends AbstractDiscoveryComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryDataSourceComponentInfo info = getComponentParametersInfo(request);
        String label = info.getConnectTo();

        var ds = resolveDataSource(request, label);
        List<?> products = ds.result().map(SearchResult::products).orElse(List.of());
        PaginationModel pagination = ds.result()
                .map(r -> new PaginationModel(r.total(), r.page(), r.pageSize()))
                .orElse(new PaginationModel(0L, 0, 0));

        setModelAndAttribute(request, "products", products);
        setModelAndAttribute(request, "pagination", pagination);
    }
}
