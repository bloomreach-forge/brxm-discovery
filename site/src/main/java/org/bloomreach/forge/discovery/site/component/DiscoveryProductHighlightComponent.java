package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductHighlightComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a curated product showcase from up to 4 hand-picked
 * {@code brxdis:productDetailDocument} pickers. Each product is fetched
 * individually from Discovery by its {@code brxdis:productId}.
 */
@ParametersInfo(type = DiscoveryProductHighlightComponentInfo.class)
public class DiscoveryProductHighlightComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryProductHighlightComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryProductHighlightComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = lookupService(HstDiscoveryService.class);

        List<ProductSummary> products = new ArrayList<>();
        for (String path : new String[]{
                info.getDocument1(), info.getDocument2(),
                info.getDocument3(), info.getDocument4()}) {
            if (path != null && !path.isBlank()) {
                DiscoveryProductDetailBean bean = getHippoBeanForPath(
                        request, path, DiscoveryProductDetailBean.class);
                if (bean != null && bean.getProductId() != null && !bean.getProductId().isBlank()) {
                    svc.fetchProduct(request, bean.getProductId()).ifPresent(products::add);
                }
            }
        }

        if (products.isEmpty()) {
            HstRequestContext ctx = request.getRequestContext();
            if (ctx != null && ctx.isChannelManagerPreviewRequest()) {
                request.setAttribute("brxdis_warning",
                        "No products configured. Select Product Detail Documents in component properties.");
            }
        }

        request.setModel("products", products);
        request.setAttribute("products", products);
        log.debug("ProductHighlight returned {} products", products.size());
    }
}
