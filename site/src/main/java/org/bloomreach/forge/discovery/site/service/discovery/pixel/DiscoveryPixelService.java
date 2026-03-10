package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;

/**
 * Fires server-side Discovery pixel events asynchronously after each result is fetched.
 * Implementations must swallow all errors — pixel failure must never affect page rendering.
 * <p>
 * Callers must pass a resolved {@link PixelFlags} instance; passing {@link PixelFlags#DISABLED}
 * guarantees that no pixel traffic is produced regardless of env/channel configuration.
 */
public interface DiscoveryPixelService {

    void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryConfig config,
                         String clientIp, ClientContext ctx, PixelFlags flags);

    void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                           String clientIp, ClientContext ctx, PixelFlags flags);

    void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                         String clientIp, ClientContext ctx, PixelFlags flags);

    void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl, String url,
                                  DiscoveryConfig config, String clientIp,
                                  ClientContext ctx, PixelFlags flags);
}
