package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;

import java.util.Optional;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;

public interface DiscoveryClient {

    AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryConfig config);

    SearchResponse search(SearchQuery query, DiscoveryConfig config);

    SearchResponse category(CategoryQuery query, DiscoveryConfig config);

    RecommendationResult recommend(RecQuery query, DiscoveryConfig config);

    Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryConfig config);

    String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryConfig config,
                                String clientIp, String userAgent);

    String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                                  String clientIp, String userAgent);

    String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                                String clientIp, String userAgent);

    String buildProductPageViewPixelPath(String pid, String brUid2, String refUrl, String url,
                                         DiscoveryConfig config, String clientIp, String userAgent);

    void firePixelEvent(String pixelPath, DiscoveryConfig config);
}
