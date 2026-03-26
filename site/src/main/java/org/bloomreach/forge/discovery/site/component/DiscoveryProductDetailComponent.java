package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductDetailBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductDetailComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@ParametersInfo(type = DiscoveryProductDetailComponentInfo.class)
public class DiscoveryProductDetailComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryProductDetailComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);

        DiscoveryProductDetailComponentInfo info = getComponentParametersInfo(request);
        String label = info.getLabel();

        DiscoveryProductDetailBean document = getHippoBeanForPath(request, info.getDocument(),
                DiscoveryProductDetailBean.class);
        request.setModel("document", document);

        String pid = getPublicRequestParameter(request, info.getProductUrlParam());

        // Stage 2 — Document bean (picker-driven; overrides only when the document has a non-blank productId.
        // A document with an unset productId must NOT wipe out a valid URL param from stage 1.)
        if (document != null) {
            String docPid = document.getProductId();
            if (docPid != null && !docPid.isBlank()) {
                pid = docPid;
            }
        }

        // Stage 3 — Page content bean property (auto-detection from document)
        if (pid == null || pid.isBlank()) {
            pid = resolvePidFromBean(request, info);
        }

        // Expose resolved pid and label for template unconditionally (before any early returns)
        request.setModel(DiscoveryModelKeys.PID, pid != null ? pid : "");
        request.setModel(DiscoveryModelKeys.LABEL, label);

        if (pid == null || pid.isBlank()) {
            // Mark label present so downstream components know PDP ran (just no PID resolved)
            DiscoveryRequestCache.markProductDetailBandPresent(request, label);
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                    "No product ID resolved. Select a 'Product Detail Document' in component properties, " +
                    "or ensure the page content bean has a '" + info.getProductPidProperty() + "' property, " +
                    "or pass '?pid=' in the URL.");
            }
            request.setModel(DiscoveryModelKeys.PRODUCT, null);
            return;
        }

        HstDiscoveryService svc = getDiscoveryService();

        Optional<ProductSummary> found = svc.fetchProduct(request, pid);
        ProductSummary product = found.orElse(null);
        request.setModel(DiscoveryModelKeys.PRODUCT, product);

        DiscoveryRequestCache.markProductDetailBandPresent(request, label);
        if (product != null) {
            DiscoveryRequestCache.putProductResult(request, label, product);
        }

        log.debug("PDP pid='{}' product={} label='{}'", pid, product != null ? product.id() : "null", label);
    }

    /**
     * Hook for testability — extracts PID from the page content bean's JCR property.
     * Override in tests to avoid mocking HstRequestContext + HippoBean chains.
     */
    protected String resolvePidFromBean(HstRequest request, DiscoveryProductDetailComponentInfo info) {
        HstRequestContext ctx = request.getRequestContext();
        if (ctx == null) {
            return null;
        }
        HippoBean pageBean = ctx.getContentBean();
        if (pageBean == null) {
            return null;
        }
        String pidProp = info.getProductPidProperty();
        if (pidProp == null || pidProp.isBlank()) {
            return null;
        }
        return pageBean.getSingleProperty(pidProp);
    }

}
