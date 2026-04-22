package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductDetailComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Discovery Product Detail component.
 *
 * <p>Requires a linked {@code brxdis:productDetailDocument}. The document dictates
 * the product ID mode:
 * <ul>
 *   <li><b>Pinned</b> - {@code brxdis:productId} is non-blank: use that ID directly.</li>
 *   <li><b>Dynamic</b> - {@code brxdis:productId} is blank: read the {@code ?pid=} URL parameter.</li>
 * </ul>
 *
 * <p>If no document is configured the component renders nothing and shows a warning
 * in Experience Manager edit mode.
 */
@ParametersInfo(type = DiscoveryProductDetailComponentInfo.class)
public class DiscoveryProductDetailComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryProductDetailComponent.class);

    @Override
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        DiscoveryProductDetailComponentInfo info = getComponentParametersInfo(request);

        DiscoveryProductDetailBean document = getHippoBeanForPath(request, info.getDocument(),
                DiscoveryProductDetailBean.class);
        request.setModel(DiscoveryModelKeys.DOCUMENT, document);

        // Document is required - no silent URL-param fallback without a document
        if (document == null) {
            DiscoveryRequestCache.markProductDetailRendered(request);
            request.setModel(DiscoveryModelKeys.PRODUCT, null);
            return;
        }

        // Document dictates mode: non-blank = Pinned, blank = Dynamic (URL-driven)
        String docPid = document.getProductId();
        String pid;
        if (docPid != null && !docPid.isBlank()) {
            pid = docPid;
        } else {
            pid = resolveUrlParam(request, info.getProductUrlParam());
        }

        request.setModel(DiscoveryModelKeys.PID, pid != null ? pid : "");

        if (pid == null || pid.isBlank()) {
            DiscoveryRequestCache.markProductDetailRendered(request);
            if (isEditMode(request)) {
                String param = info.getProductUrlParam();
                request.setAttribute("brxdis_warning",
                    "Product document is in Dynamic mode but no product ID was found. " +
                    "Ensure the sitemap maps the URL segment to parameter '" + param +
                    "', or pass '?" + param + "=' as a query string.");
            }
            request.setModel(DiscoveryModelKeys.PRODUCT, null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();
        Optional<ProductSummary> found = svc.fetchProduct(request, pid);
        ProductSummary product = found.orElse(null);
        request.setModel(DiscoveryModelKeys.PRODUCT, product);

        DiscoveryRequestCache.markProductDetailRendered(request);
        if (product != null) {
            DiscoveryRequestCache.putProductResult(request, product);
        }

        log.debug("PDP pid='{}' product={}", pid, product != null ? product.id() : "null");
    }
}
