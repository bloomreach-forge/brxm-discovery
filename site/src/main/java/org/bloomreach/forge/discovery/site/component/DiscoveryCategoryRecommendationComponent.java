package org.bloomreach.forge.discovery.site.component;

import org.bloomreach.forge.discovery.site.beans.DiscoveryCategoryRecommendationBean;
import org.bloomreach.forge.discovery.site.component.constants.DiscoveryModelKeys;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryCategoryRecommendationComponentInfo;
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
 * Discovery Recommendations - category context.
 *
 * <p>Reads widget and context configuration from a linked {@code brxdis:categoryRecommendationDocument},
 * where all settings are stored as JSON in {@code brxdis:config} (authored via the recommendation wizard).
 * If {@code contextCategoryId} is null in the document, falls back to the {@code ?category=} URL parameter.
 * If no category ID is available from either source, the component renders empty products.
 *
 * <p>Catalog entry: {@code /category-recommendations} - "Discovery Category Recommendations".
 */
@ParametersInfo(type = DiscoveryCategoryRecommendationComponentInfo.class)
public class DiscoveryCategoryRecommendationComponent extends AbstractDiscoveryRecommendationComponent {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCategoryRecommendationComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) throws HstComponentException {
        super.doBeforeRender(request, response);
        DiscoveryCategoryRecommendationComponentInfo info = getComponentParametersInfo(request);
        HstDiscoveryService svc = getDiscoveryService();

        DiscoveryCategoryRecommendationBean document = getHippoBeanForPath(
                request, info.getDocument(), DiscoveryCategoryRecommendationBean.class);
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

        String catId = cfg.contextCategoryId() != null && !cfg.contextCategoryId().isBlank()
                ? cfg.contextCategoryId()
                : getPublicRequestParameter(request, "category");

        if (catId == null || catId.isBlank()) {
            if (isEditMode(request)) {
                request.setAttribute("brxdis_warning",
                        "No category configured. Set a category in the recommendation document " +
                        "or pass a '?category=' URL parameter.");
            }
            request.setModel(DiscoveryModelKeys.PRODUCTS, List.of());
            request.setModel(DiscoveryModelKeys.WIDGET_ID, cfg.widgetId());
            return;
        }

        int    limit  = getPublicRequestParameterAsInt(request, LIMIT_PARAM, info.getLimit());
        String fields = getPublicRequestParameter(request, FIELDS_PARAM);
        String filter = getPublicRequestParameter(request, FILTER_PARAM);

        RecommendationResult recResult = svc.recommend(
                request, cfg.widgetId(), "category", null, catId, null, limit, fields, filter);
        List<ProductSummary> products = recResult.products();
        String resolvedWidgetId = recResult.widgetId() != null && !recResult.widgetId().isBlank()
                ? recResult.widgetId() : cfg.widgetId();

        request.setModel(DiscoveryModelKeys.PRODUCTS, products);
        request.setModel(DiscoveryModelKeys.WIDGET_ID, resolvedWidgetId);
        request.setModel(DiscoveryModelKeys.WIDGET_TYPE, recResult.widgetType());
        request.setModel(DiscoveryModelKeys.WIDGET_RESULT_ID, recResult.widgetResultId());
        request.setModel(DiscoveryModelKeys.WIDGET_QUERY, catId);

        log.debug("Category recommendations widget '{}' for category '{}' returned {} products",
                resolvedWidgetId, catId, products.size());
    }
}
