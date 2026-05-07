package org.bloomreach.forge.discovery.site.service.discovery.autosuggest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.exception.SearchException;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.autosuggest.dto.AutosuggestResponse;
import org.bloomreach.forge.discovery.site.service.discovery.dto.ProductDocMapper;

import java.util.ArrayList;
import java.util.List;

public class AutosuggestResponseMapper {

    private final ObjectMapper objectMapper;

    public AutosuggestResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AutosuggestResult toAutosuggestResult(String json) {
        return toAutosuggestResult(parse(json));
    }

    private AutosuggestResult toAutosuggestResult(AutosuggestResponse dto) {
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
                            .map(ProductDocMapper::toProductSummary)
                            .forEach(productSuggestions::add);
                }
            }
        }

        return new AutosuggestResult(originalQuery,
                List.copyOf(querySuggestions),
                List.copyOf(attributeSuggestions),
                List.copyOf(productSuggestions));
    }

    private AutosuggestResponse parse(String json) {
        try {
            return objectMapper.readValue(json, AutosuggestResponse.class);
        } catch (JsonProcessingException e) {
            throw new SearchException("Failed to parse Discovery autosuggest response", e);
        }
    }
}
