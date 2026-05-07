package org.bloomreach.forge.discovery.site.service.discovery.search;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;

import java.util.List;
import java.util.Optional;

public interface SearchApiClient {

    SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx);

    Optional<ProductSummary> fetchProduct(String pid, String url, String fields, DiscoveryCredentials credentials, ClientContext ctx);

    List<ProductSummary> visualSearch(VisualSearchQuery query, DiscoveryCredentials credentials, ClientContext ctx);
}
