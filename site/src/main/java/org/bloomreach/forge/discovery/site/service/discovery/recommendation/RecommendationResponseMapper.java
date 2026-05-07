package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDocMapper;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.dto.RecommendationResponse;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationResponseMapper {

    private final ObjectMapper objectMapper;

    public RecommendationResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RecommendationResult toRecommendationResult(String json) {
        return toRecommendationResult(parse(json));
    }

    public Map<String, String> toWidgetTypeMap(String json) {
        try {
            return toWidgetTypeMapFromNode(objectMapper.readTree(json));
        } catch (JsonProcessingException e) {
            throw new SearchException("Failed to parse Discovery response", e);
        }
    }

    private RecommendationResult toRecommendationResult(RecommendationResponse dto) {
        if (dto.response() == null || dto.response().docs() == null) {
            return RecommendationResult.of(List.of());
        }
        List<ProductSummary> products = dto.response().docs().stream()
                .map(ProductDocMapper::toProductSummary)
                .toList();
        String wid = dto.metadata() != null && dto.metadata().widget() != null
                ? dto.metadata().widget().id() : null;
        String wty = dto.metadata() != null && dto.metadata().widget() != null
                ? dto.metadata().widget().type() : null;
        String wrid = dto.metadata() != null && dto.metadata().widget() != null
                ? dto.metadata().widget().rid() : null;
        return new RecommendationResult(wid, wty, wrid, products);
    }

    private Map<String, String> toWidgetTypeMapFromNode(JsonNode root) {
        JsonNode widgets = root.path("response").path("widgets");
        if (!widgets.isArray()) {
            return Map.of();
        }
        Map<String, String> result = new HashMap<>();
        for (JsonNode widget : widgets) {
            String id = widget.path("id").asText(null);
            String type = widget.path("type").asText(null);
            if (id != null && !id.isBlank() && type != null && !type.isBlank()) {
                result.put(id, type);
            }
        }
        return Map.copyOf(result);
    }

    private RecommendationResponse parse(String json) {
        try {
            return objectMapper.readValue(json, RecommendationResponse.class);
        } catch (JsonProcessingException e) {
            throw new SearchException("Failed to parse Discovery recommendation response", e);
        }
    }
}
