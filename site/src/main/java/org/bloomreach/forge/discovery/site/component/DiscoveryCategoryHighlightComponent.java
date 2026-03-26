package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryHighlight;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryHighlightComponentInfo;
import org.bloomreach.forge.discovery.site.platform.CategoryPreviewCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.platform.SearchRequestOptions;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a grid of category navigation tiles sourced from up to 4 JCR
 * {@code brxdis:categoryDocument} pickers. Each tile links to the category
 * browse page via the {@code categoryId} field.
 */
@ParametersInfo(type = DiscoveryCategoryHighlightComponentInfo.class)
public class DiscoveryCategoryHighlightComponent extends AbstractDiscoveryComponent {

    private static final int MAX_SLOTS = 4; // supports up to 4 curated category slots

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryHighlightComponentInfo info = getComponentParametersInfo(request);

        List<DiscoveryCategoryBean> categoryBeans = new ArrayList<>(MAX_SLOTS);
        for (String path : new String[]{
                info.getDocument1(), info.getDocument2(),
                info.getDocument3(), info.getDocument4()}) {
            if (path != null && !path.isBlank()) {
                DiscoveryCategoryBean bean = getHippoBeanForPath(request, path, DiscoveryCategoryBean.class);
                if (bean != null) {
                    categoryBeans.add(bean);
                }
            }
        }

        if (categoryBeans.isEmpty() && isEditMode(request)) {
            request.setAttribute("brxdis_warning",
                    "No categories configured. Select Category Documents in component properties.");
        }

        List<CategoryHighlight> categories = categoryBeans.stream()
                .map(b -> new CategoryHighlight(b.getCategoryId(), b.getDisplayName(), b.getProductPreviewCount()))
                .toList();

        request.setModel(DiscoveryModelKeys.CATEGORIES, categories);
        request.setModel(DiscoveryModelKeys.PREVIEW_PRODUCTS, fetchPreviewProducts(request, categoryBeans));
    }

    protected CategoryPreviewCache getCategoryPreviewCache() {
        return lookupService(CategoryPreviewCache.class);
    }

    private Map<String, List<ProductSummary>> fetchPreviewProducts(
            HstRequest request, List<DiscoveryCategoryBean> categories) {
        Map<String, List<ProductSummary>> result = new LinkedHashMap<>();
        HstDiscoveryService svc = getDiscoveryService();
        if (svc == null) return result;
        CategoryPreviewCache cache = getCategoryPreviewCache();
        for (DiscoveryCategoryBean cat : categories) {
            int count = cat.getProductPreviewCount();
            String catId = cat.getCategoryId();
            if (count <= 0 || catId == null || catId.isBlank()) continue;
            cache.get(catId, count).ifPresentOrElse(
                    products -> result.put(catId, products),
                    () -> {
                        SearchResponse resp = svc.browse(request, catId,
                                SearchRequestOptions.of("highlight-preview-" + catId, count));
                        List<ProductSummary> products = resp.result().products();
                        cache.put(catId, count, products);
                        result.put(catId, products);
                    });
        }
        return result;
    }
}
