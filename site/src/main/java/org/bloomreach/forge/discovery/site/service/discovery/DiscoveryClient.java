package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;

import java.util.List;
import java.util.Optional;

public interface DiscoveryClient {

    AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryConfig config);

    SearchResult search(SearchQuery query, DiscoveryConfig config);

    SearchResult category(CategoryQuery query, DiscoveryConfig config);

    List<ProductSummary> recommend(RecQuery query, DiscoveryConfig config);

    Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryConfig config);

    String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryConfig config);

    String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryConfig config);

    String buildWidgetPixelPath(RecQuery query, List<ProductSummary> products, DiscoveryConfig config);

    void firePixelEvent(String pixelPath, DiscoveryConfig config);
}
