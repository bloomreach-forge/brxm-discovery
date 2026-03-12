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

final class DiscoveryRecommendationClient {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryRecommendationClient.class);

    private static final String SEARCH_RESOURCE_SPACE = "discoverySearchAPI";
    private static final String PATHWAYS_RESOURCE_SPACE = "discoveryPathwaysAPI";

    private final DiscoveryResourceExecutor executor;
    private final DiscoveryResponseMapper responseMapper;
    private final DiscoveryRequestFactory requestFactory;

    DiscoveryRecommendationClient(DiscoveryResourceExecutor executor,
                                  DiscoveryResponseMapper responseMapper,
                                  DiscoveryRequestFactory requestFactory) {
        this.executor = executor;
        this.responseMapper = responseMapper;
        this.requestFactory = requestFactory;
    }

    RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx) {
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            return recommendV2(query, credentials, ctx);
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
}
