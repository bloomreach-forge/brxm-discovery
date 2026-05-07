package org.bloomreach.forge.discovery.site.service.discovery.search;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestHeaders;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestLogging;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.bloomreach.forge.discovery.visual.model.VisualSearchQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Optional;

public class SearchApiClientImpl implements SearchApiClient {

    private static final Logger log = LoggerFactory.getLogger(SearchApiClientImpl.class);

    private final DiscoveryTransport transport;
    private final DiscoveryConfigProvider configProvider;
    private final SearchResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;

    public SearchApiClientImpl(DiscoveryTransport transport, DiscoveryConfigProvider configProvider,
                               SearchResponseMapper responseMapper, DiscoveryRequestFactory requestFactory) {
        this.transport = transport;
        this.configProvider = configProvider;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    @Override
    public SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.search(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery search [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().baseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            SearchResponse response = responseMapper.toSearchResponse(json, query.page(), query.pageSize());
            log.debug("Discovery search returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (DiscoveryException e) {
            log.error("Discovery search failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw e;
        }
    }

    @Override
    public SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.category(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery category browse [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().baseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            SearchResponse response = responseMapper.toBrowseResponse(json, query.page(), query.pageSize(), query.categoryId());
            log.debug("Discovery category returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (DiscoveryException e) {
            log.error("Discovery category failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw e;
        }
    }

    @Override
    public Optional<ProductSummary> fetchProduct(String pid, String url, String fields,
                                                  DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.productLookup(pid, url, fields, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery fetchProduct [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().baseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            SearchResult result = responseMapper.toSearchResult(json, 0, 1);
            log.debug("Discovery fetchProduct pid='{}' found={} [request_id={}]",
                    pid, !result.products().isEmpty(), requestLog.requestId());
            return result.products().isEmpty() ? Optional.empty() : Optional.of(result.products().get(0));
        } catch (DiscoveryException e) {
            log.warn("Discovery fetchProduct failed [request_id={}] for pid '{}': {}",
                    requestLog.requestId(), pid, e.getMessage());
            throw e;
        }
    }

    @Override
    public List<ProductSummary> visualSearch(VisualSearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.visualSearch(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery visual search [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().pathwaysBaseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forPathways(uri, credentials, ctx));
            List<ProductSummary> products = responseMapper.toVisualSearchProducts(json);
            log.debug("Discovery visual search returned {} products [request_id={}]",
                    products.size(), requestLog.requestId());
            return products;
        } catch (DiscoveryException e) {
            log.error("Discovery visual search failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw e;
        }
    }
}
