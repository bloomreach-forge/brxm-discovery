package org.bloomreach.forge.discovery.site.service.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.site.service.discovery.dto.AutosuggestResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.CampaignDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetCounts;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetFieldDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FacetValueDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.FieldStatsEntryDto;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDoc;
import org.bloomreach.forge.discovery.site.service.discovery.dto.RecommendationResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.SearchApiResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.StatsDto;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.Campaign;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.FieldStats;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
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

    public SearchResponse toSearchResponse(Resource resource, int page, int pageSize) {
        SearchApiResponse dto = parse(resource, SearchApiResponse.class);
        SearchResult result = toSearchResult(dto, page, pageSize);
        Map<String, FieldStats> stats = toStats(dto.stats());
        String redirectUrl = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedUrl() : null;
        String redirectQuery = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedQuery() : null;
        Campaign campaign = toCampaign(dto.campaign());
        return new SearchResponse(result, new SearchMetadata(stats, dto.didYouMean(), dto.autoCorrectQuery(),
                redirectUrl, redirectQuery, campaign));
    }

    public SearchResult toSearchResult(Resource resource, int page, int pageSize) {
        return toSearchResult(parse(resource, SearchApiResponse.class), page, pageSize);
    }

    private SearchResult toSearchResult(SearchApiResponse dto, int page, int pageSize) {
        long total = dto.response() != null ? dto.response().numFound() : 0L;
        List<ProductSummary> products = dto.response() != null && dto.response().docs() != null
                ? dto.response().docs().stream().map(this::toProductSummary).toList()
                : List.of();
        Map<String, Facet> facets = toFacets(dto.facetCounts());
        return new SearchResult(products, total, page, pageSize, facets);
    }

    public RecommendationResult toRecommendationResult(Resource resource) {
        RecommendationResponse dto = parse(resource, RecommendationResponse.class);
        if (dto.response() == null || dto.response().docs() == null) {
            return RecommendationResult.of(List.of());
        }
        List<ProductSummary> products = dto.response().docs().stream().map(this::toProductSummary).toList();
        String wrid = dto.metadata() != null && dto.metadata().widget() != null
                ? dto.metadata().widget().rid() : null;
        return new RecommendationResult(wrid, products);
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

    private Campaign toCampaign(CampaignDto dto) {
        if (dto == null) return null;
        return new Campaign(dto.id(), dto.campaignName(), dto.htmlText(), dto.bannerUrl(), dto.imageUrl());
    }

    private static void putIfPresent(Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
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

    private Map<String, FieldStats> toStats(StatsDto statsDto) {
        if (statsDto == null || statsDto.statsFields() == null || statsDto.statsFields().isEmpty()) {
            return Map.of();
        }
        Map<String, FieldStats> result = new LinkedHashMap<>();
        for (Map.Entry<String, FieldStatsEntryDto> entry : statsDto.statsFields().entrySet()) {
            FieldStatsEntryDto e = entry.getValue();
            result.put(entry.getKey(), new FieldStats(e.min(), e.max(), e.mean(), e.count()));
        }
        return Map.copyOf(result);
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
