package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResult;

/**
 * Fires server-side Discovery pixel events asynchronously after each result is fetched.
 * Implementations must swallow all errors — pixel failure must never affect page rendering.
 * <p>
 * Callers must pass a resolved {@link PixelFlags} instance; passing {@link PixelFlags#DISABLED}
 * guarantees that no pixel traffic is produced regardless of env/channel configuration.
 */
public interface DiscoveryPixelService {

    default void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                 String clientIp, ClientContext ctx, PixelFlags flags) {
        fireSearchEvent(query, result, credentials, null, clientIp, ctx, flags);
    }

    void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                         String title, String clientIp, ClientContext ctx, PixelFlags flags);

    default void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                   String clientIp, ClientContext ctx, PixelFlags flags) {
        fireCategoryEvent(query, result, credentials, null, clientIp, ctx, flags);
    }

    void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                           String title, String clientIp, ClientContext ctx, PixelFlags flags);

    default void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                 String clientIp, ClientContext ctx, PixelFlags flags) {
        fireWidgetEvent(query, result, credentials, null, null, clientIp, ctx, flags);
    }

    void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                         String pageType, String title, String clientIp, ClientContext ctx, PixelFlags flags);

    default void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl, String url,
                                          DiscoveryCredentials credentials, String clientIp,
                                          ClientContext ctx, PixelFlags flags) {
        fireProductPageViewEvent(pid, prodName, brUid2, refUrl, null, url, null,
                credentials, clientIp, ctx, flags);
    }

    void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl, String origRefUrl,
                                  String url, String title,
                                  DiscoveryCredentials credentials, String clientIp,
                                  ClientContext ctx, PixelFlags flags);

    void fireDeferredEvent(DeferredPixelEvent event, DiscoveryCredentials credentials, String clientIp,
                           ClientContext ctx, PixelFlags flags);
}
