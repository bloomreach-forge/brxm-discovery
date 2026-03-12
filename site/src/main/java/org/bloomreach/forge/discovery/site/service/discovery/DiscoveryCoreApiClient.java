package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

final class DiscoveryCoreApiClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryCoreApiClient.class);

    private static final String SEARCH_RESOURCE_SPACE = "discoverySearchAPI";
    private static final String AUTOSUGGEST_RESOURCE_SPACE = "discoveryAutosuggestAPI";

    private final DiscoveryResourceExecutor executor;
    private final DiscoveryResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;

    DiscoveryCoreApiClient(DiscoveryResourceExecutor executor,
                           DiscoveryResponseMapper responseMapper,
                           DiscoveryRequestFactory requestFactory) {
        this.executor = executor;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    AutosuggestResult autosuggest(AutosuggestQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.autosuggest(query, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery autosuggest [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolve(AUTOSUGGEST_RESOURCE_SPACE, path, ctx);
            AutosuggestResult result = responseMapper.toAutosuggestResult(resource);
            log.debug("Discovery autosuggest returned {} query suggestions [request_id={}]",
                    result.querySuggestions().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery autosuggest failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Autosuggest request failed: " + e.getMessage(), e);
        }
    }

    SearchResponse search(SearchQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.search(query, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery search [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolve(SEARCH_RESOURCE_SPACE, path, ctx);
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery search returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (ResourceException e) {
            log.error("Discovery search failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Search request failed: " + e.getMessage(), e);
        }
    }

    SearchResponse category(CategoryQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.category(query, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery category browse [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolve(SEARCH_RESOURCE_SPACE, path, ctx);
            SearchResponse response = responseMapper.toSearchResponse(resource, query.page(), query.pageSize());
            log.debug("Discovery category returned {} results [request_id={}]",
                    response.result().total(), requestLog.requestId());
            return response;
        } catch (ResourceException e) {
            log.error("Discovery category failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new SearchException("Category request failed: " + e.getMessage(), e);
        }
    }

    Optional<ProductSummary> fetchProduct(String pid, String url, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.productLookup(pid, url, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery fetchProduct [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolve(SEARCH_RESOURCE_SPACE, path, ctx);
            SearchResult result = responseMapper.toSearchResult(resource, 0, 1);
            log.debug("Discovery fetchProduct pid='{}' found={} [request_id={}]",
                    pid, !result.products().isEmpty(), requestLog.requestId());
            return result.products().isEmpty() ? Optional.empty() : Optional.of(result.products().get(0));
        } catch (ResourceException e) {
            log.warn("Discovery fetchProduct failed [request_id={}] for pid '{}': {}",
                    requestLog.requestId(), pid, e.getMessage());
            throw new SearchException("fetchProduct request failed: " + e.getMessage(), e);
        }
    }
}
