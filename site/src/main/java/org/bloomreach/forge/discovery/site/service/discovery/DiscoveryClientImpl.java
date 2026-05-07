package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.site.service.discovery.autosuggest.AutosuggestApiClient;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.event.PixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.RecommendationApiClient;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.site.service.discovery.search.SearchApiClient;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;

import java.util.List;
import java.util.Optional;

public class DiscoveryClientImpl implements DiscoveryApiClient, DiscoveryPixelTransport {

    private final SearchApiClient searchClient;
    private final AutosuggestApiClient autosuggestClient;
    private final RecommendationApiClient recommendationClient;
    private final DiscoveryPixelTransport pixelTransport;

    public DiscoveryClientImpl(SearchApiClient searchClient, AutosuggestApiClient autosuggestClient,
                               RecommendationApiClient recommendationClient,
                               DiscoveryPixelTransport pixelTransport) {
        this.searchClient = searchClient;
        this.autosuggestClient = autosuggestClient;
        this.recommendationClient = recommendationClient;
        this.pixelTransport = pixelTransport;
    }

    @Override
    public SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return searchClient.search(query, credentials, ctx);
    }

    @Override
    public SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return searchClient.category(query, credentials, ctx);
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, String fields, DiscoveryCredentials credentials, ClientContext ctx) {
        return searchClient.fetchProduct(pid, url, fields, credentials, ctx);
    }

    @Override
    public List<ProductSummary> visualSearch(VisualSearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return searchClient.visualSearch(query, credentials, ctx);
    }

    @Override
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return autosuggestClient.autosuggest(query, credentials, ctx);
    }

    @Override
    public RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return recommendationClient.recommend(query, credentials, ctx);
    }

    @Override
    public String buildPath(PixelEvent event, DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        return pixelTransport.buildPath(event, credentials, clientIp, flags);
    }

    @Override
    public void fire(String path, ClientContext ctx, PixelFlags flags) {
        pixelTransport.fire(path, ctx, flags);
    }

    public static String toV2WidgetType(String rawType) {
        return DiscoveryRequestFactory.toV2WidgetType(rawType);
    }

    static String redactPath(String path) {
        return DiscoveryRequestLogging.redactPath(path);
    }

    static String requestId(String path) {
        return DiscoveryRequestLogging.requestId(path);
    }
}
