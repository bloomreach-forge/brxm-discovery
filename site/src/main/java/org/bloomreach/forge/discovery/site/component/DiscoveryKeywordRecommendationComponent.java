package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryKeywordRecommendationBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryKeywordRecommendationComponentInfo;
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
 * Discovery Recommendations - keyword-driven.
 *
 * <p>Calls the Bloomreach Discovery keyword widget endpoint, passing a search query. The query
 * is resolved in two modes (configured via the recommendation wizard):
 * <ul>
 *   <li><b>specific</b>: uses the fixed keyword authored in the recommendation document</li>
 *   <li><b>url</b>: reads the {@code ?q=} request parameter at runtime (falls back to empty)</li>
 * </ul>
 *
 * <p>Catalog entry: {@code /keyword-recommendations} - "Discovery Recommendations - Keyword".
 */
@ParametersInfo(type = DiscoveryKeywordRecommendationComponentInfo.class)
public class DiscoveryKeywordRecommendationComponent extends AbstractDiscoveryRecommendationComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryKeywordRecommendationComponent.class);

    @Override
    protected void doDiscoveryBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        DiscoveryKeywordRecommendationComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();

        DiscoveryKeywordRecommendationBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryKeywordRecommendationBean.class);
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

        int limit = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter = getPublicRequestParameter(request, FILTER_PARAM);

        String contextQuery = resolveContextQuery(request, cfg);

        RecommendationResult recResult = svc.recommend(
                request, cfg.widgetId(), cfg.widgetType(),
                null, null, null, contextQuery, limit, fields, filter);
        List<ProductSummary> products = recResult.products();
        String resolvedWidgetId = recResult.widgetId() != null && !recResult.widgetId().isBlank()
                ? recResult.widgetId() : cfg.widgetId();

        request.setModel(DiscoveryModelKeys.PRODUCTS, products);
        request.setModel(DiscoveryModelKeys.WIDGET_ID, resolvedWidgetId);
        request.setModel(DiscoveryModelKeys.WIDGET_TYPE, recResult.widgetType());
        request.setModel(DiscoveryModelKeys.WIDGET_RESULT_ID, recResult.widgetResultId());

        log.debug("Keyword recommendations widget '{}' type='{}' query='{}' returned {} products",
                resolvedWidgetId, cfg.widgetType(), contextQuery, products.size());
    }

    private static String resolveContextQuery(HstRequest request, DiscoveryRecommendationConfig cfg) {
        if ("url".equals(cfg.contextQueryMode())) {
            return request.getRequestContext().getServletRequest().getParameter("q");
        }
        return cfg.contextQuery();
    }
}
