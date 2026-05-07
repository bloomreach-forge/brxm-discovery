package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.DiscoveryException;
import org.bloomreach.forge.discovery.exception.RecommendationException;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.request.DiscoveryRequestFactory;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestHeaders;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryRequestLogging;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.transport.DiscoveryTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RecommendationApiClientImpl implements RecommendationApiClient {

    private static final Logger log = LoggerFactory.getLogger(RecommendationApiClientImpl.class);

    private final DiscoveryTransport transport;
    private final DiscoveryConfigProvider configProvider;
    private final RecommendationResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;
    private final ConcurrentMap<String, Map<String, String>> widgetTypeCache = new ConcurrentHashMap<>();

    public RecommendationApiClientImpl(DiscoveryTransport transport, DiscoveryConfigProvider configProvider,
                                       RecommendationResponseMapper responseMapper,
                                       DiscoveryRequestFactory requestFactory) {
        this.transport = transport;
        this.configProvider = configProvider;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    @Override
    public RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            return recommendV2(resolveWidgetType(query, credentials, ctx), credentials, ctx);
        }
        return recommendV1(query, credentials, ctx);
    }

    private RecommendationResult recommendV1(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.recommendationV1(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery recommendations v1 [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().baseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            RecommendationResult result = responseMapper.toRecommendationResult(json);
            log.debug("Discovery recommendations v1 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (DiscoveryException e) {
            log.error("Discovery recommendations v1 failed [request_id={}] for path {}: {}",
                    requestLog.requestId(), requestLog.redactedPath(), e.getMessage());
            throw new RecommendationException("Recommendation request failed: " + e.getMessage(), e);
        }
    }

    private RecommendationResult recommendV2(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.recommendationV2(query, credentials).toRelativePath();
        DiscoveryRequestLogging.RequestLogContext requestLog = DiscoveryRequestLogging.requestLog(path);
        log.debug("Discovery recommendations v2 (Pathways) [request_id={}]: {}", requestLog.requestId(), requestLog.redactedPath());
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().pathwaysBaseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forPathways(uri, credentials, ctx));
            RecommendationResult result = responseMapper.toRecommendationResult(json);
            log.debug("Discovery recommendations v2 returned {} products [request_id={}]",
                    result.products().size(), requestLog.requestId());
            return result;
        } catch (DiscoveryException e) {
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
        return new RecQuery(resolvedType, widgetId, query.contextProductId(), query.catId(), query.contextPageType(),
                query.limit(), query.fields(), query.filters(), query.url(), query.refUrl(),
                query.brUid2(), query.origRefUrl(), query.query(), query.viewId());
    }

    private Map<String, String> widgetTypes(DiscoveryCredentials credentials, ClientContext ctx) {
        String cacheKey = credentials.accountId() + "|" + credentials.domainKey();
        return widgetTypeCache.computeIfAbsent(cacheKey, ignored -> loadWidgetTypes(credentials, ctx));
    }

    private Map<String, String> loadWidgetTypes(DiscoveryCredentials credentials, ClientContext ctx) {
        String path = requestFactory.merchantWidgets(credentials).toRelativePath();
        try {
            URI uri = DiscoveryRequestHeaders.buildUri(configProvider.settings().baseUri(), path);
            String json = transport.execute(DiscoveryRequestHeaders.forSearch(uri, ctx));
            return responseMapper.toWidgetTypeMap(json);
        } catch (DiscoveryException e) {
            log.warn("Failed to resolve recommendation widget types: {}", Objects.toString(e.getMessage(), "unknown error"));
            return Map.of();
        }
    }
}
