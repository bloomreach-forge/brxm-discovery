package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.RecommendationException;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class DiscoveryRecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRecommendationClient.class);

    private static final String SEARCH_RESOURCE_SPACE = "discoverySearchAPI";
    private static final String PATHWAYS_RESOURCE_SPACE = "discoveryPathwaysAPI";

    private final DiscoveryResourceExecutor executor;
    private final DiscoveryResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;
    private final ConcurrentMap<String, Map<String, String>> widgetTypeCache = new ConcurrentHashMap<>();

    DiscoveryRecommendationClient(DiscoveryResourceExecutor executor,
                                  DiscoveryResponseMapper responseMapper,
                                  DiscoveryRequestFactory requestFactory) {
        this.executor = executor;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            return recommendV2(resolveWidgetType(query, credentials, ctx), credentials, ctx);
        }
        return recommendV1(query, credentials, ctx);
    }

    static String toV2WidgetType(String rawType) {
        return DiscoveryRequestFactory.toV2WidgetType(rawType);
    }

    private RecommendationResult recommendV1(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.recommendationV1(query, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery recommendations v1 [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolve(SEARCH_RESOURCE_SPACE, path, ctx);
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v1 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v1 failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new RecommendationException("Recommendation request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV2(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.recommendationV2(query, credentials));
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery recommendations v2 (Pathways) [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            Resource resource = executor.resolvePathways(PATHWAYS_RESOURCE_SPACE, path, credentials, ctx);
            RecommendationResult result = responseMapper.toRecommendationResult(resource);
            log.debug("Discovery recommendations v2 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (ResourceException e) {
            log.error("Discovery recommendations v2 failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new RecommendationException("Pathways recommendation request failed: " + e.getMessage(), e);
        }
    }

    private RecQuery resolveWidgetType(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        if (query.widgetType() != null && !query.widgetType().isBlank()) {
            return query;
        }
        String widgetId = query.widgetId();
        if (widgetId == null || widgetId.isBlank()) {
            return query;
        }
        String resolvedType = widgetTypes(credentials, ctx).get(widgetId);
        if (resolvedType == null || resolvedType.isBlank()) {
            log.warn("Unable to resolve widget type for widget '{}'; falling back to default Pathways route", widgetId);
            return query;
        }
        return new RecQuery(resolvedType, widgetId, query.contextProductId(), query.contextPageType(),
                query.limit(), query.fields(), query.filters(), query.url(), query.refUrl(),
                query.brUid2(), query.origRefUrl());
    }

    private Map<String, String> widgetTypes(DiscoveryCredentials credentials, ClientContext ctx) {
        String cacheKey = credentials.accountId() + "|" + credentials.domainKey();
        return widgetTypeCache.computeIfAbsent(cacheKey, ignored -> loadWidgetTypes(credentials, ctx));
    }

    private Map<String, String> loadWidgetTypes(DiscoveryCredentials credentials, ClientContext ctx) {
        String path = DiscoveryRequestPaths.toRelativePath(requestFactory.merchantWidgets(credentials));
        try {
            return responseMapper.toWidgetTypeMap(executor.resolve(SEARCH_RESOURCE_SPACE, path, ctx));
        } catch (ResourceException e) {
            log.warn("Failed to resolve recommendation widget types: {}", Objects.toString(e.getMessage(), "unknown error"));
            return Map.of();
        }
    }
}
