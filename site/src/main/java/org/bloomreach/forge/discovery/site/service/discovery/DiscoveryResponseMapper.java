package org.bloomreach.forge.discovery.site.service.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.site.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.dto.AutosuggestResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetCounts;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetFieldDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetValueDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDoc;
import org.bloomreach.forge.discovery.site.service.discovery.dto.RecommendationResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.SearchApiResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.WidgetDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.WidgetListResponse;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.WidgetInfo;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.onehippo.cms7.crisp.api.resource.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DiscoveryResponseMapper {

    private final ObjectMapper objectMapper;

    public DiscoveryResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchResult toSearchResult(Resource resource, int page, int pageSize) {
        SearchApiResponse dto = parse(resource, SearchApiResponse.class);
        long total = dto.response() != null ? dto.response().numFound() : 0L;
        List<ProductSummary> products = dto.response() != null && dto.response().docs() != null
                ? dto.response().docs().stream().map(this::toProductSummary).toList()
                : List.of();
        Map<String, Facet> facets = toFacets(dto.facetCounts());
        return new SearchResult(products, total, page, pageSize, facets);
    }

    public List<ProductSummary> toRecommendationResult(Resource resource) {
        RecommendationResponse dto = parse(resource, RecommendationResponse.class);
        if (dto.response() == null || dto.response().docs() == null) {
            return List.of();
        }
        return dto.response().docs().stream().map(this::toProductSummary).toList();
    }

    public List<WidgetInfo> toWidgetList(Resource resource) {
        WidgetListResponse dto = parse(resource, WidgetListResponse.class);
        if (dto.response() == null || dto.response().widgets() == null) {
            return List.of();
        }
        return dto.response().widgets().stream().map(this::toWidgetInfo).toList();
    }

    public AutosuggestResult toAutosuggestResult(Resource resource) {
        AutosuggestResponse dto = parse(resource, AutosuggestResponse.class);
        String originalQuery = dto.queryContext() != null ? dto.queryContext().originalQuery() : null;
        List<String> querySuggestions = new ArrayList<>();
        List<AutosuggestResult.AttributeSuggestion> attributeSuggestions = new ArrayList<>();
        List<ProductSummary> productSuggestions = new ArrayList<>();

        if (dto.suggestionGroups() != null) {
            for (AutosuggestResponse.SuggestionGroup group : dto.suggestionGroups()) {
                if (group.querySuggestions() != null) {
                    group.querySuggestions().stream()
                            .map(AutosuggestResponse.QuerySuggestion::query)
                            .forEach(querySuggestions::add);
                }
                if (group.attributeSuggestions() != null) {
                    group.attributeSuggestions().stream()
                            .map(a -> new AutosuggestResult.AttributeSuggestion(
                                    a.name(), a.value(), a.attributeType()))
                            .forEach(attributeSuggestions::add);
                }
                if (group.searchSuggestions() != null) {
                    group.searchSuggestions().stream()
                            .map(this::toProductSummary)
                            .forEach(productSuggestions::add);
                }
            }
        }

        return new AutosuggestResult(originalQuery,
                List.copyOf(querySuggestions),
                List.copyOf(attributeSuggestions),
                List.copyOf(productSuggestions));
    }

    private ProductSummary toProductSummary(ProductDoc doc) {
        Map<String, Object> attrs = new LinkedHashMap<>();
        putIfPresent(attrs, "brand", doc.brand());
        putIfPresent(attrs, "description", doc.description());
        if (doc.salePrice() != null) {
            attrs.put("sale_price", doc.salePrice());
        }
        return new ProductSummary(doc.pid(), doc.title(), doc.url(), doc.thumbImage(),
                doc.price(), doc.currency(), Map.copyOf(attrs));
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private WidgetInfo toWidgetInfo(WidgetDto dto) {
        return new WidgetInfo(dto.id(), dto.name(), dto.type(), dto.enabled(), dto.description());
    }

    private Map<String, Facet> toFacets(FacetCounts facetCounts) {
        if (facetCounts == null || facetCounts.facets() == null) {
            return Map.of();
        }
        Map<String, Facet> result = new LinkedHashMap<>();
        for (FacetFieldDto field : facetCounts.facets()) {
            if (field.name() == null) continue;
            List<FacetValue> values = field.value() != null
                    ? field.value().stream().map(this::toFacetValue).toList()
                    : List.of();
            result.put(field.name(), new Facet(field.name(), field.type(), values));
        }
        return result;
    }

    private FacetValue toFacetValue(FacetValueDto dto) {
        String name = dto.catName() != null ? dto.catName() : dto.name();
        return new FacetValue(name, dto.count(), dto.catId(), dto.crumb(), dto.treePath(), dto.parent());
    }

    private <T> T parse(Resource resource, Class<T> type) {
        try {
            JsonNode node = (JsonNode) resource.getNodeData();
            return objectMapper.treeToValue(node, type);
        } catch (JsonProcessingException e) {
            throw new SearchException("Failed to parse Discovery response", e);
        }
    }
}
