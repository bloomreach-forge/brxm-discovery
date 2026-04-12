package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryProductRecommendationBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryProductRecommendationComponentInfo;
import org.bloomreach.forge.discovery.site.platform.HstDiscoveryService;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.DiscoveryRecommendationConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Discovery Recommendations - product (item) context.
 *
 * <p>Reads widget and context configuration from a linked {@code brxdis:productRecommendationDocument},
 * where all settings are stored as JSON in {@code brxdis:config} (authored via the recommendation wizard).
 * If {@code contextProductId} is null in the document, falls back to the {@code ?pid=} URL parameter.
 *
 * <p>Catalog entry: {@code /product-recommendations} - "Discovery Product Recommendations".
 */
@ParametersInfo(type = DiscoveryProductRecommendationComponentInfo.class)
public class DiscoveryProductRecommendationComponent extends AbstractDiscoveryRecommendationComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryProductRecommendationComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryProductRecommendationComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();

        DiscoveryProductRecommendationBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryProductRecommendationBean.class);
        request.setModel("document", document);
        request.setModel(DiscoveryModelKeys.SHOW_PRICE, info.isShowPrice());
        request.setModel(DiscoveryModelKeys.SHOW_DESCRIPTION, info.isShowDescription());

        Optional<DiscoveryRecommendationConfig> cfgOpt = document != null ? document.getConfig() : Optional.empty();
        if (cfgOpt.isEmpty()) {
            request.setModel(DiscoveryModelKeys.PRODUCTS, List.of());
            request.setModel(DiscoveryModelKeys.WIDGET_ID, "");
            return;
        }
        DiscoveryRecommendationConfig cfg = cfgOpt.get();

        String pid = cfg.contextProductId() != null && !cfg.contextProductId().isBlank()
                ? cfg.contextProductId()
                : getPublicRequestParameter(request, "pid");

        int    limit  = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter = getPublicRequestParameter(request, FILTER_PARAM);

        RecommendationResult recResult = svc.recommend(
                request, cfg.widgetId(), cfg.widgetType(), pid, null, null, limit, fields, filter);
        List<ProductSummary> products = recResult.products();
        String resolvedWidgetId = recResult.widgetId() != null && !recResult.widgetId().isBlank()
                ? recResult.widgetId() : cfg.widgetId();

        request.setModel(DiscoveryModelKeys.PRODUCTS, products);
        request.setModel(DiscoveryModelKeys.WIDGET_ID, resolvedWidgetId);
        request.setModel(DiscoveryModelKeys.WIDGET_TYPE, recResult.widgetType());
        request.setModel(DiscoveryModelKeys.WIDGET_RESULT_ID, recResult.widgetResultId());
        request.setModel(DiscoveryModelKeys.WIDGET_QUERY, pid);

        log.debug("Product recommendations widget '{}' type='{}' pid='{}' returned {} products",
                resolvedWidgetId, cfg.widgetType(), pid, products.size());
    }
}
