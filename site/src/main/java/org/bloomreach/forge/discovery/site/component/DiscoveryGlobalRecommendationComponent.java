package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryGlobalRecommendationBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryGlobalRecommendationComponentInfo;
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
 * Discovery Recommendations - context-free (global / personalized).
 *
 * <p>Reads widget and type configuration from a linked {@code brxdis:globalRecommendationDocument},
 * where all settings are stored as JSON in {@code brxdis:config} (authored via the recommendation wizard).
 * No product or category context is used - the widget type (global/personalized) is set in the document.
 *
 * <p>Catalog entry: {@code /global-recommendations} - "Discovery Recommendations - Trending / Personalized".
 */
@ParametersInfo(type = DiscoveryGlobalRecommendationComponentInfo.class)
public class DiscoveryGlobalRecommendationComponent extends AbstractDiscoveryRecommendationComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryGlobalRecommendationComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryGlobalRecommendationComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();

        DiscoveryGlobalRecommendationBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryGlobalRecommendationBean.class);
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

        int    limit  = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter = getPublicRequestParameter(request, FILTER_PARAM);

        RecommendationResult recResult = svc.recommend(
                request, cfg.widgetId(), cfg.widgetType(), null, null, null, limit, fields, filter);
        List<ProductSummary> products = recResult.products();
        String resolvedWidgetId = recResult.widgetId() != null && !recResult.widgetId().isBlank()
                ? recResult.widgetId() : cfg.widgetId();

        request.setModel(DiscoveryModelKeys.PRODUCTS, products);
        request.setModel(DiscoveryModelKeys.WIDGET_ID, resolvedWidgetId);
        request.setModel(DiscoveryModelKeys.WIDGET_TYPE, recResult.widgetType());
        request.setModel(DiscoveryModelKeys.WIDGET_RESULT_ID, recResult.widgetResultId());

        log.debug("Global recommendations widget '{}' type='{}' returned {} products",
                resolvedWidgetId, cfg.widgetType(), products.size());
    }
}
