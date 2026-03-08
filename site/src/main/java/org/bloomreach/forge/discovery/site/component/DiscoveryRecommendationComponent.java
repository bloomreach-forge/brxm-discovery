package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@ParametersInfo(type = DiscoveryRecommendationComponentInfo.class)
public class DiscoveryRecommendationComponent extends AbstractDiscoveryComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRecommendationComponent.class);
    static final String WIDGET_ID_PARAM = "widgetId";
    static final String CONTEXT_PRODUCT_PARAM = "contextProductId";
    static final String CONTEXT_PAGE_TYPE_PARAM = "contextPageType";
    static final String LIMIT_PARAM = "limit";
    static final String FIELDS_PARAM = "fields";
    static final String FILTER_PARAM = "filter";

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryRecommendationComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();
        final String documentPath = info.getDocument();
        final String dataSource   = info.getDataSource();
        final String band         = info.getBand();

        // Resolve the recommendation document (component param wins, then URL-path content bean)
        DiscoveryRecommendationBean document = getHippoBeanForPath(request, documentPath, DiscoveryRecommendationBean.class);
        request.setAttribute("document", document);
        request.setAttribute("showPrice", info.isShowPrice());
        request.setAttribute("showDescription", info.isShowDescription());
        setModelAndAttribute(request, "dataSource", dataSource);
        setModelAndAttribute(request, "dataBand", band);

        // Widget ID: document field → URL request param
        String widgetId = document != null && document.getWidgetId() != null && !document.getWidgetId().isBlank()
                ? document.getWidgetId()
                : resolveWidgetId(request);

        // Nothing configured — return empty rather than fire an invalid API call
        if (widgetId == null || widgetId.isBlank()) {
            setModelAndAttribute(request, "products", List.of());
            setModelAndAttribute(request, "widgetId", "");
            return;
        }

        String contextProductId;

        if ("productDetailBand".equals(dataSource)) {
            // ── Band mode: PDP component publishes resolved PID to the cache ──────
            boolean bandPresent = DiscoveryRequestCache.isProductDetailBandPresent(request, band);
            if (!bandPresent) {
                if (isEditMode(request)) {
                    request.setAttribute("brxdis_warning",
                        "No product detail band '" + band + "' found. Add a Product Detail component " +
                        "with band='" + band + "' to this page.");
                }
                setModelAndAttribute(request, "products", List.of());
                setModelAndAttribute(request, "widgetId", widgetId);
                return;
            }
            java.util.Optional<ProductSummary> cached = DiscoveryRequestCache.getProductResult(request, band);
            if (cached.isEmpty()) {
                if (isEditMode(request)) {
                    request.setAttribute("brxdis_warning",
                        "Product detail band '" + band + "' is present but no product ID was resolved. " +
                        "Ensure the Product Detail component has a valid product configured.");
                }
                setModelAndAttribute(request, "products", List.of());
                setModelAndAttribute(request, "widgetId", widgetId);
                return;
            }
            contextProductId = cached.get().id();
        } else {
            // ── Standalone mode: existing 4-stage PID resolution ──────────────────

            // 1. URL param (developer override / testing)
            contextProductId = getPublicRequestParameter(request, CONTEXT_PRODUCT_PARAM);

            // 2. Component param (editor-set static PID via Channel Manager)
            if (contextProductId == null || contextProductId.isBlank()) {
                contextProductId = info.getContextProductId();
            }

            // 3. Page content bean property (PDP auto-detection, e.g. brxdis:pid)
            if (contextProductId == null || contextProductId.isBlank()) {
                HstRequestContext ctx = request.getRequestContext();
                HippoBean pageBean = ctx != null ? ctx.getContentBean() : null;
                if (pageBean != null && !(pageBean instanceof DiscoveryRecommendationBean)) {
                    String pidProp = info.getContextProductPidProperty();
                    if (pidProp != null && !pidProp.isBlank()) {
                        contextProductId = pageBean.getSingleProperty(pidProp);
                    }
                }
            }
            // 4. URL "pid" param — PDP page passes product ID this way
            if (contextProductId == null || contextProductId.isBlank()) {
                contextProductId = getPublicRequestParameter(request, "pid");
            }
            // 5. null — guard in HstDiscoveryService handles gracefully
        }

        String contextPageType = getPublicRequestParameter(request, CONTEXT_PAGE_TYPE_PARAM);
        int    limit           = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields          = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter          = getPublicRequestParameter(request, FILTER_PARAM);

        List<ProductSummary> products = svc.recommend(
                request, widgetId, null, contextProductId, contextPageType, limit, fields, filter);

        setModelAndAttribute(request, "products", products);
        setModelAndAttribute(request, "widgetId", widgetId);

        log.debug("Recommendations widget '{}' dataSource='{}' band='{}' returned {} products",
                widgetId, dataSource, band, products.size());
    }

    /**
     * Fallback widget ID from the URL request parameter.
     */
    String resolveWidgetId(HstRequest request) {
        return getPublicRequestParameter(request, WIDGET_ID_PARAM);
    }
}
