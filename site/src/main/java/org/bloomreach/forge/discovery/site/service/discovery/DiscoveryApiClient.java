package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;

import java.util.Optional;

public interface DiscoveryApiClient {

    AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryCredentials credentials, ClientContext ctx);
}
