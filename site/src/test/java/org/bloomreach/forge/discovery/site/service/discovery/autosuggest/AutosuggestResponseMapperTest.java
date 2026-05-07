package org.bloomreach.forge.discovery.site.service.discovery.autosuggest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.search.model.AutosuggestResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutosuggestResponseMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AutosuggestResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AutosuggestResponseMapper(MAPPER);
    }

    @Test
    void toAutosuggestResult_parsesQueryAndProductAndAttributeSuggestions() {
        String json = """
                {
                  "queryContext": {"originalQuery": "shi"},
                  "suggestionGroups": [{
                    "catalogName": "products_en",
                    "view": "store",
                    "querySuggestions": [
                      {"query": "shirts", "displayText": "shirts"},
                      {"query": "shipping", "displayText": "shipping"}
                    ],
                    "attributeSuggestions": [
                      {"name": "brand", "value": "Nike", "attributeType": "text"}
                    ],
                    "searchSuggestions": [
                      {"pid":"p1","title":"Blue Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img.jpg","price":29.99,"currency":"USD",
                       "brand":"Nike","description":"Cotton shirt"}
                    ]
                  }]
                }
                """;

        AutosuggestResult result = mapper.toAutosuggestResult(json);

        assertEquals("shi", result.originalQuery());
        assertEquals(List.of("shirts", "shipping"), result.querySuggestions());
        assertEquals(1, result.attributeSuggestions().size());
        assertEquals("brand", result.attributeSuggestions().get(0).name());
        assertEquals("Nike",  result.attributeSuggestions().get(0).value());
        assertEquals("text",  result.attributeSuggestions().get(0).attributeType());
        assertEquals(1, result.productSuggestions().size());
        assertEquals("p1",   result.productSuggestions().get(0).id());
        assertEquals("Nike", result.productSuggestions().get(0).attributes().get("brand"));
    }

    @Test
    void toAutosuggestResult_multipleSuggestionGroups_flattenedIntoSingleResult() {
        String json = """
                {
                  "queryContext": {"originalQuery": "lap"},
                  "suggestionGroups": [
                    {
                      "querySuggestions": [{"query": "laptop", "displayText": "laptop"}],
                      "attributeSuggestions": [],
                      "searchSuggestions": [
                        {"pid":"p1","title":"Laptop","url":"https://shop.com/laptop",
                         "thumb_image":"http://img.jpg","price":999.99,"currency":"USD"}
                      ]
                    },
                    {
                      "querySuggestions": [{"query": "lap desk", "displayText": "lap desk"}],
                      "attributeSuggestions": [{"name":"category","value":"Desks","attributeType":"text"}],
                      "searchSuggestions": [
                        {"pid":"p2","title":"Lap Desk","url":"https://shop.com/desk",
                         "thumb_image":"http://img2.jpg","price":49.99,"currency":"USD"}
                      ]
                    }
                  ]
                }
                """;

        AutosuggestResult result = mapper.toAutosuggestResult(json);

        assertEquals("lap", result.originalQuery());
        assertEquals(List.of("laptop", "lap desk"), result.querySuggestions());
        assertEquals(1, result.attributeSuggestions().size());
        assertEquals(2, result.productSuggestions().size());
        assertEquals("p1", result.productSuggestions().get(0).id());
        assertEquals("p2", result.productSuggestions().get(1).id());
    }

    @Test
    void toAutosuggestResult_emptySuggestionGroups_returnsEmptyLists() {
        AutosuggestResult result = mapper.toAutosuggestResult("""
                {
                  "queryContext": {"originalQuery": "xyz"},
                  "suggestionGroups": []
                }
                """);

        assertEquals("xyz", result.originalQuery());
        assertTrue(result.querySuggestions().isEmpty());
        assertTrue(result.attributeSuggestions().isEmpty());
        assertTrue(result.productSuggestions().isEmpty());
    }

    @Test
    void toAutosuggestResult_nullSuggestionGroups_returnsEmptyLists() {
        AutosuggestResult result = mapper.toAutosuggestResult("""
                {"queryContext": {"originalQuery": "abc"}}
                """);

        assertEquals("abc", result.originalQuery());
        assertTrue(result.querySuggestions().isEmpty());
        assertTrue(result.attributeSuggestions().isEmpty());
        assertTrue(result.productSuggestions().isEmpty());
    }

    @Test
    void toAutosuggestResult_nullQueryContext_returnsNullOriginalQuery() {
        AutosuggestResult result = mapper.toAutosuggestResult("""
                {"suggestionGroups": []}
                """);

        assertNull(result.originalQuery());
    }
}
