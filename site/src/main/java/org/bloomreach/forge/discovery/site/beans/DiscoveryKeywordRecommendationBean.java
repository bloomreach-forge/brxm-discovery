package org.bloomreach.forge.discovery.site.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.DiscoveryRecommendationConfig;
import org.hippoecm.hst.content.beans.Node;
import org.hippoecm.hst.content.beans.standard.HippoDocument;

import java.util.Optional;

@Node(jcrType = "brxdis:keywordRecommendationDocument")
public class DiscoveryKeywordRecommendationBean extends HippoDocument {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String getDisplayName() {
        return getSingleProperty("brxdis:displayName");
    }

    @JsonIgnore
    public Optional<DiscoveryRecommendationConfig> getConfig() {
        String json = getSingleProperty("brxdis:config");
        if (json == null || json.isBlank()) return Optional.empty();
        try {
            return Optional.of(MAPPER.readValue(json, DiscoveryRecommendationConfig.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
