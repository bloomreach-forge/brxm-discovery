package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResult;

public interface DiscoveryPixelTransport {

    String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                String clientIp, PixelFlags flags);

    String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                  String clientIp, PixelFlags flags);

    String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                String clientIp, PixelFlags flags);

    String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl, String url,
                                         DiscoveryCredentials credentials, String clientIp, PixelFlags flags);

    void firePixelEvent(String pixelPath, ClientContext ctx, PixelFlags flags);
}
