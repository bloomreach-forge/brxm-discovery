package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductHighlightComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders a curated product showcase from up to 4 hand-picked
 * {@code brxdis:productDetailDocument} pickers. Each product is fetched
 * individually from Discovery by its {@code brxdis:productId}.
 */
@ParametersInfo(type = DiscoveryProductHighlightComponentInfo.class)
public class DiscoveryProductHighlightComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryProductHighlightComponent.class);
    private static final int MAX_SLOTS = 4; // supports up to 4 curated product slots

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryProductHighlightComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();

        List<DiscoveryProductDetailBean> productBeans = new ArrayList<>(MAX_SLOTS);
        List<ProductSummary> products = new ArrayList<>(MAX_SLOTS);
        for (String path : new String[]{
                info.getDocument1(), info.getDocument2(),
                info.getDocument3(), info.getDocument4()}) {
            DiscoveryProductDetailBean bean = (path != null && !path.isBlank())
                    ? getHippoBeanForPath(request, path, DiscoveryProductDetailBean.class)
                    : null;
            productBeans.add(bean);
            ProductSummary product = (bean != null && bean.getProductId() != null && !bean.getProductId().isBlank())
                    ? svc.fetchProduct(request, bean.getProductId()).orElse(null)
                    : null;
            products.add(product);
        }

        boolean anyProduct = products.stream().anyMatch(Objects::nonNull);
        if (!anyProduct && isEditMode(request)) {
            request.setAttribute("brxdis_warning",
                    "No products configured. Select Product Detail Documents in component properties.");
        }

        setModelAndAttribute(request, "products", products);
        setModelAndAttribute(request, "productBeans", productBeans);
        log.debug("ProductHighlight returned {} of {} products", products.stream().filter(Objects::nonNull).count(), MAX_SLOTS);
    }
}
