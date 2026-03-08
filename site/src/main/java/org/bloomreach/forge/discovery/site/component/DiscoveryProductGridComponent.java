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
 * Configurable via component parameter {@code dataSource}: {@code "search"} (default) or
 * {@code "category"}.
 */
@ParametersInfo(type = DiscoveryDataSourceComponentInfo.class)
public class DiscoveryProductGridComponent extends AbstractDiscoveryComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryDataSourceComponentInfo info = getComponentParametersInfo(request);
        String dataSource = info.getDataSource();
        String band = info.getBandName();

        boolean isCategory = "category".equals(dataSource);

        Optional<SearchResult> result = isCategory
                ? DiscoveryRequestCache.getCategoryResult(request, band)
                : DiscoveryRequestCache.getSearchResult(request, band);

        boolean bandConnected = isCategory
                ? DiscoveryRequestCache.isCategoryBandPresent(request, band)
                : DiscoveryRequestCache.isSearchBandPresent(request, band);
        if (!bandConnected) {
            Class<?> dataComponentClass = isCategory
                    ? DiscoveryCategoryComponent.class
                    : DiscoverySearchComponent.class;
            bandConnected = isBandConfiguredOnPage(request, band, dataComponentClass);
        }
        warnIfMissingDataSource(request, !bandConnected, isCategory, band);

        List<?> products = result.map(SearchResult::products).orElse(List.of());
        PaginationModel pagination = result
                .map(r -> new PaginationModel(r.total(), r.page(), r.pageSize()))
                .orElse(new PaginationModel(0L, 0, 0));

        setModelAndAttribute(request, "products", products);
        setModelAndAttribute(request, "dataBand", band);
        setModelAndAttribute(request, "bandConnected", bandConnected);
        setModelAndAttribute(request, "pagination", pagination);
    }
}
