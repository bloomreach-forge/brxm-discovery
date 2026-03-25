package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.DeferredPixelEvent;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHint;

import java.util.Optional;

/**
 * Thin Discovery facade that delegates core API, recommendation, and pixel work
 * to focused collaborators while keeping the public addon API stable.
 */
public class DiscoveryClientImpl implements DiscoveryClient {

    private final DiscoveryCoreApiClient coreApiClient;
    private final DiscoveryRecommendationClient recommendationClient;
    private final DiscoveryPixelTransport pixelTransport;

    public DiscoveryClientImpl(ResourceServiceBroker broker, DiscoveryResponseMapper responseMapper) {
        this(broker, responseMapper, new DiscoveryRequestFactory());
    }

    DiscoveryClientImpl(ResourceServiceBroker broker, DiscoveryResponseMapper responseMapper,
                        DiscoveryRequestFactory requestFactory) {
        DiscoveryResourceExecutor executor = new DiscoveryResourceExecutor(broker);
        this.coreApiClient = new DiscoveryCoreApiClient(executor, responseMapper, requestFactory);
        this.recommendationClient = new DiscoveryRecommendationClient(executor, responseMapper, requestFactory);
        this.pixelTransport = new DefaultDiscoveryPixelTransport(executor);
    }

    @Override
    public AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return coreApiClient.autosuggest(query, credentials, ctx);
    }

    @Override
    public SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return coreApiClient.search(query, credentials, ctx);
    }

    @Override
    public SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return coreApiClient.category(query, credentials, ctx);
    }

    @Override
    public RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        return recommendationClient.recommend(query, credentials, ctx);
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryCredentials credentials, ClientContext ctx) {
        return coreApiClient.fetchProduct(pid, url, credentials, ctx);
    }

    @Override
    public String buildSearchPixelPath(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                       String title, String clientIp, PixelFlags flags) {
        return pixelTransport.buildSearchPixelPath(query, result, credentials, title, clientIp, flags);
    }

    @Override
    public String buildCategoryPixelPath(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                         String title, String clientIp, PixelFlags flags) {
        return pixelTransport.buildCategoryPixelPath(query, result, credentials, title, clientIp, flags);
    }

    @Override
    public String buildWidgetPixelPath(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                       String pageType, String title, String clientIp, PixelFlags flags) {
        return pixelTransport.buildWidgetPixelPath(query, result, credentials, pageType, title, clientIp, flags);
    }

    @Override
    public String buildProductPageViewPixelPath(String pid, String prodName, String brUid2, String refUrl,
                                                String origRefUrl, String url, String title,
                                                DiscoveryCredentials credentials, String clientIp, PixelFlags flags) {
        return pixelTransport.buildProductPageViewPixelPath(pid, prodName, brUid2, refUrl, origRefUrl, url, title,
                credentials, clientIp, flags);
    }

    @Override
    public String buildDeferredEventPixelPath(DeferredPixelEvent event, DiscoveryCredentials credentials,
                                              String clientIp, PixelFlags flags) {
        return pixelTransport.buildDeferredEventPixelPath(event, credentials, clientIp, flags);
    }

    @Override
    public void firePixelEvent(String pixelPath, ClientContext ctx, PixelFlags flags) {
        pixelTransport.firePixelEvent(pixelPath, ctx, flags);
    }

    public static String toV2WidgetType(String rawType) {
        return DiscoveryRecommendationClient.toV2WidgetType(rawType);
    }

    static String redactPath(String path) {
        return DiscoveryRequestLogging.redactPath(path);
    }

    static ExchangeHint buildHint(ClientContext ctx) {
        return DiscoveryExchangeHints.buildHint(ctx);
    }

    static ExchangeHint buildV2Hint(DiscoveryCredentials credentials, ClientContext ctx) {
        return DiscoveryExchangeHints.buildV2Hint(credentials, ctx);
    }

    static String requestId(String path) {
        return DiscoveryRequestLogging.requestId(path);
    }
}
