package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;

/**
 * Fires server-side Discovery pixel events asynchronously after each result is fetched.
 * Implementations must swallow all errors — pixel failure must never affect page rendering.
 */
public interface DiscoveryPixelService {

    void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryConfig config,
                         String clientIp, String userAgent);

    void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                           String clientIp, String userAgent);

    void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                         String clientIp, String userAgent);

    void fireProductPageViewEvent(String pid, String brUid2, String refUrl, String url,
                                  DiscoveryConfig config, String clientIp, String userAgent);
}
