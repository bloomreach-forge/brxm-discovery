package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryRecommendationBean;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.DiscoveryRequestCache;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
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
import java.util.Optional;

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
        final String dataSource = info.getDataSource();
        final String label      = info.getConnectTo();

        DiscoveryRecommendationBean document = getHippoBeanForPath(request, info.getDocument(), DiscoveryRecommendationBean.class);
        request.setAttribute("document", document);
        request.setAttribute("showPrice", info.isShowPrice());
        request.setAttribute("showDescription", info.isShowDescription());
        setModelAndAttribute(request, "dataSource", dataSource);
        setModelAndAttribute(request, "label", label);

        String widgetId = resolveWidgetId(document, request);
        if (widgetId == null || widgetId.isBlank()) {
            setModelAndAttribute(request, "products", List.of());
            setModelAndAttribute(request, "widgetId", "");
            return;
        }

        Optional<String> pidResolution = resolveContextProductId(request, info, dataSource, label, widgetId);
        if (pidResolution.isEmpty()) return; // productDetailBand aborted — empty state already set
        String contextProductId = pidResolution.get().isEmpty() ? null : pidResolution.get();

        String contextPageType = getPublicRequestParameter(request, CONTEXT_PAGE_TYPE_PARAM);
        int    limit           = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields          = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter          = getPublicRequestParameter(request, FILTER_PARAM);

        RecommendationResult recResult = svc.recommend(
                request, widgetId, null, contextProductId, contextPageType, limit, fields, filter, label);
        List<ProductSummary> products = recResult.products();

        setModelAndAttribute(request, "products", products);
        setModelAndAttribute(request, "widgetId", widgetId);

        log.debug("Recommendations widget '{}' dataSource='{}' label='{}' returned {} products",
                widgetId, dataSource, label, products.size());
    }

    /** Widget ID: document field wins, then URL request param. */
    private String resolveWidgetId(DiscoveryRecommendationBean doc, HstRequest request) {
        String fromDoc = doc != null && doc.getWidgetId() != null && !doc.getWidgetId().isBlank()
                ? doc.getWidgetId() : null;
        return fromDoc != null ? fromDoc : getPublicRequestParameter(request, WIDGET_ID_PARAM);
    }

    /**
     * Returns the resolved PID wrapped in Optional, or {@code Optional.empty()} to abort.
     * On abort this method has already set the empty-state model on {@code request}.
     * Present value may be empty-string ("") meaning no context product — caller maps to null.
     */
    private Optional<String> resolveContextProductId(HstRequest request,
                                                       DiscoveryRecommendationComponentInfo info,
                                                       String dataSource, String label, String widgetId) {
        if ("productDetailBand".equals(dataSource)) {
            return resolveProductDetailBandPid(request, label, widgetId);
        }
        return resolveStandalonePid(request, info);
    }

    /** productDetailBand mode: PID from PDP cache or PPR backfill. Empty = abort. */
    private Optional<String> resolveProductDetailBandPid(HstRequest request, String label, String widgetId) {
        boolean labelPresent = DiscoveryRequestCache.isProductDetailBandPresent(request, label);
        if (!labelPresent) {
            if (isIsolatedComponentRender(request)) {
                Optional<String> backfilled = backfillProductDetailPid(request, label);
                if (backfilled.isPresent()) return backfilled;
                String pid = getPublicRequestParameter(request, "pid");
                if (pid == null || pid.isBlank()) {
                    setModelAndAttribute(request, "products", List.of());
                    setModelAndAttribute(request, "widgetId", widgetId);
                    return Optional.empty();
                }
                return Optional.of(pid);
            } else {
                if (isEditMode(request)) {
                    request.setAttribute("brxdis_warning",
                        "No product detail label '" + label + "' found. Add a Product Detail component " +
                        "with label='" + label + "' to this page.");
                }
                setModelAndAttribute(request, "products", List.of());
                setModelAndAttribute(request, "widgetId", widgetId);
                return Optional.empty();
            }
        }
        Optional<ProductSummary> cached = DiscoveryRequestCache.getProductResult(request, label);
        if (cached.isEmpty()) {
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                    "Product detail label '" + label + "' is present but no product ID was resolved. " +
                    "Ensure the Product Detail component has a valid product configured.");
            }
            setModelAndAttribute(request, "products", List.of());
            setModelAndAttribute(request, "widgetId", widgetId);
            return Optional.empty();
        }
        return Optional.of(cached.get().id());
    }

    /**
     * Standalone mode: 5-stage PID resolution (URL param → component param → page bean → URL pid → none).
     * Always returns present — empty string means no context product (HstDiscoveryService handles gracefully).
     */
    private Optional<String> resolveStandalonePid(HstRequest request, DiscoveryRecommendationComponentInfo info) {
        // 1. URL param (developer override / testing)
        String pid = getPublicRequestParameter(request, CONTEXT_PRODUCT_PARAM);
        if (pid != null && !pid.isBlank()) return Optional.of(pid);

        // 2. Component param (editor-set static PID via Channel Manager)
        pid = info.getContextProductId();
        if (pid != null && !pid.isBlank()) return Optional.of(pid);

        // 3. Page content bean property (PDP auto-detection, e.g. brxdis:pid)
        HstRequestContext ctx = request.getRequestContext();
        HippoBean pageBean = ctx != null ? ctx.getContentBean() : null;
        if (pageBean != null && !(pageBean instanceof DiscoveryRecommendationBean)) {
            String pidProp = info.getContextProductPidProperty();
            if (pidProp != null && !pidProp.isBlank()) {
                pid = pageBean.getSingleProperty(pidProp);
                if (pid != null && !pid.isBlank()) return Optional.of(pid);
            }
        }

        // 4. URL "pid" param — PDP page passes product ID this way
        pid = getPublicRequestParameter(request, "pid");
        // 5. No PID resolved — empty string so caller passes null to service (handled gracefully)
        return Optional.of(pid != null && !pid.isBlank() ? pid : "");
    }
}
