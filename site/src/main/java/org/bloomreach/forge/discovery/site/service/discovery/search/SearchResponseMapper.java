package org.bloomreach.forge.discovery.site.service.discovery.search;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.search.model.Campaign;
import org.bloomreach.forge.discovery.search.model.Facet;
import org.bloomreach.forge.discovery.search.model.FacetValue;
import org.bloomreach.forge.discovery.search.model.FieldStats;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.search.model.SearchMetadata;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDocMapper;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.CampaignDto;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.FacetCounts;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.FacetFieldDto;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.FacetValueDto;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.FieldStatsEntryDto;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.SearchApiResponse;
import org.bloomreach.forge.discovery.site.service.discovery.search.dto.StatsDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SearchResponseMapper {

    private final ObjectMapper objectMapper;

    public SearchResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SearchResponse toSearchResponse(String json, int page, int pageSize) {
        return toSearchResponse(parse(json), page, pageSize);
    }

    public SearchResponse toBrowseResponse(String json, int page, int pageSize, String categoryId) {
        return toBrowseResponse(parse(json), page, pageSize, categoryId);
    }

    public SearchResult toSearchResult(String json, int page, int pageSize) {
        return toSearchResult(parse(json), page, pageSize);
    }

    public List<ProductSummary> toVisualSearchProducts(String json) {
        return toSearchResult(parse(json), 0, Integer.MAX_VALUE).products();
    }

    private SearchResponse toSearchResponse(SearchApiResponse dto, int page, int pageSize) {
        SearchResult result = toSearchResult(dto, page, pageSize);
        Map<String, FieldStats> stats = toStats(dto.stats());
        String redirectUrl = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedUrl() : null;
        String redirectQuery = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedQuery() : null;
        Campaign campaign = toCampaign(dto.campaign());
        return new SearchResponse(result, new SearchMetadata(stats, dto.didYouMean(), dto.autoCorrectQuery(),
                redirectUrl, redirectQuery, campaign));
    }

    private SearchResponse toBrowseResponse(SearchApiResponse dto, int page, int pageSize, String categoryId) {
        SearchResult result = toSearchResult(dto, page, pageSize);
        Map<String, FieldStats> stats = toStats(dto.stats());
        String redirectUrl = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedUrl() : null;
        String redirectQuery = dto.keywordRedirect() != null ? dto.keywordRedirect().redirectedQuery() : null;
        Campaign campaign = toCampaign(dto.campaign());
        String categoryName = extractCategoryName(dto.categoryMap(), categoryId);
        return new SearchResponse(result, new SearchMetadata(stats, dto.didYouMean(), dto.autoCorrectQuery(),
                redirectUrl, redirectQuery, campaign, categoryName));
    }

    private static String extractCategoryName(Map<String, String> categoryMap, String categoryId) {
        if (categoryMap == null || categoryId == null) return null;
        return categoryMap.get(categoryId);
    }

    private SearchResult toSearchResult(SearchApiResponse dto, int page, int pageSize) {
        long total = dto.response() != null ? dto.response().numFound() : 0L;
        List<ProductSummary> products = dto.response() != null && dto.response().docs() != null
                ? dto.response().docs().stream().map(ProductDocMapper::toProductSummary).toList()
                : List.of();
        Map<String, Facet> facets = toFacets(dto.facetCounts());
        return new SearchResult(products, total, page, pageSize, facets);
    }

    private Campaign toCampaign(CampaignDto dto) {
        if (dto == null) return null;
        return new Campaign(dto.id(), dto.campaignName(), dto.htmlText(), dto.bannerUrl(), dto.imageUrl());
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
        return new FacetValue(name, dto.count(), dto.catId(), dto.crumb(), dto.treePath(), dto.parent(), dto.start(), dto.end());
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

    private SearchApiResponse parse(String json) {
        try {
            return objectMapper.readValue(json, SearchApiResponse.class);
        } catch (JsonProcessingException e) {
            throw new SearchException("Failed to parse Discovery search response", e);
        }
    }
}
