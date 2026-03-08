package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryDataSourceComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import java.util.Map;
import java.util.Optional;

/**
 * View component that reads facets from a parent data-fetching component's cached result.
 * Configurable via component parameter {@code dataSource}: {@code "search"} (default) or {@code "category"}.
 */
@ParametersInfo(type = DiscoveryDataSourceComponentInfo.class)
public class DiscoveryFacetComponent extends AbstractDiscoveryComponent {

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryDataSourceComponentInfo info = getComponentParametersInfo(request);
        boolean isCategory = "category".equals(info.getDataSource());
        String band = info.getBandName();
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
        Map<?, ?> facets = result.map(SearchResult::facets).orElse(Map.of());
        setModelAndAttribute(request, "facets", facets);
        setModelAndAttribute(request, "dataBand", band);
        setModelAndAttribute(request, "bandConnected", bandConnected);
    }
}
