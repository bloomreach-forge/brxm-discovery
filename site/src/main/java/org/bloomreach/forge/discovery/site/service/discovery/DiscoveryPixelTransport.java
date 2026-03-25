package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DeferredPixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResult;

public interface DiscoveryPixelTransport {

    default String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                        String clientIp, PixelFlags flags) {
        return buildSearchPixelPath(query, result, credentials, null, clientIp, flags);
    }

    String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                String title, String clientIp, PixelFlags flags);

    default String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                          String clientIp, PixelFlags flags) {
        return buildCategoryPixelPath(query, result, credentials, null, clientIp, flags);
    }

    String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                  String title, String clientIp, PixelFlags flags);

    default String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                        String clientIp, PixelFlags flags) {
        return buildWidgetPixelPath(query, result, credentials, null, null, clientIp, flags);
    }

    String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                String pageType, String title, String clientIp, PixelFlags flags);

    default String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl, String url,
                                                 DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        return buildProductPageViewPixelPath(pid, prodName, brUid2, refUrl, null, url, null,
                credentials, clientIp, flags);
    }

    String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl,
                                         String origRefUrl, String url, String title,
                                         DiscoveryCredentials credentials, String clientIp, PixelFlags flags);

    String buildDeferredEventPixelPath(DeferredPixelEvent event, DiscoveryCredentials credentials,
                                       String clientIp, PixelFlags flags);

    void firePixelEvent(String pixelPath, ClientContext ctx, PixelFlags flags);
}
