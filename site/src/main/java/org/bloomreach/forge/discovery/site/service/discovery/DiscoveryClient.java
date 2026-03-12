package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;

import java.util.Optional;
import org.bloomreach.forge.discovery.search.model.ProductSummary;

public interface DiscoveryClient {

    AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryCredentials credentials, ClientContext ctx);

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
