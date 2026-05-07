package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;

public interface RecommendationApiClient {

    RecommendationResult recommend(RecQuery query, DiscoveryCredentials credentials, ClientContext ctx);
}
