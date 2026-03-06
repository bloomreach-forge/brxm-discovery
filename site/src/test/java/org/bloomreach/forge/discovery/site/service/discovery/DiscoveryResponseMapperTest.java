package org.bloomreach.forge.discovery.site.service.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.crisp.api.resource.Resource;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryResponseMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private DiscoveryResponseMapper mapper;

    @Mock Resource resource;

    @BeforeEach
    void setUp() {
        mapper = new DiscoveryResponseMapper(MAPPER);
    }

    // ── toSearchResult ──────────────────────────────────────────────────────────

    @Test
    void toSearchResult_mapsProductsAndTotal() throws Exception {
        String json = """
                {
                  "response": {
                    "numFound": 200,
                    "docs": [
                      {"pid":"sku-001","title":"Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img1.jpg","price":19.99,"currency":"USD"},
                      {"pid":"sku-002","title":"Pants","url":"https://shop.com/pants",
                       "thumb_image":"http://img2.jpg","price":49.99,"currency":"EUR"}
                    ]
                  }
                }
                """;
        stubResource(json);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertEquals(200L, result.total());
        assertEquals(0, result.page());
        assertEquals(10, result.pageSize());
        assertEquals(2, result.products().size());

        var p1 = result.products().get(0);
        assertEquals("sku-001", p1.id());
        assertEquals("Shirt", p1.title());
        assertEquals("https://shop.com/shirt", p1.url());
        assertEquals("http://img1.jpg", p1.imageUrl());
        assertEquals(0, new java.math.BigDecimal("19.99").compareTo(p1.price()));
        assertEquals("USD", p1.currency());
    }

    @Test
    void toSearchResult_emptyDocs_returnsEmptyProductList() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertEquals(0L, result.total());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void toSearchResult_nullResponse_returnsZeroTotalAndEmptyList() throws Exception {
        stubResource("{}");

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertEquals(0L, result.total());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void toSearchResult_mapsFacets() throws Exception {
        String json = """
                {
                  "response": {"numFound": 5, "docs": []},
                  "facet_counts": {
                    "facets": [
                      {
                        "name": "brand",
                        "value": [
                          {"name": "Nike",   "count": 10},
                          {"name": "Adidas", "count": 5}
                        ]
                      }
                    ]
                  }
                }
                """;
        stubResource(json);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertTrue(result.facets().containsKey("brand"), "Facets should contain 'brand'");
        var brandFacet = result.facets().get("brand");
        assertEquals("brand", brandFacet.name());
        assertEquals(2, brandFacet.value().size());
        assertEquals("Nike",  brandFacet.value().get(0).name());
        assertEquals(10L,     brandFacet.value().get(0).count());
        assertEquals("Adidas", brandFacet.value().get(1).name());
        assertEquals(5L,       brandFacet.value().get(1).count());
    }

    @Test
    void toSearchResult_nullFacetCounts_returnsEmptyFacets() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertTrue(result.facets().isEmpty());
    }

    @Test
    void toSearchResult_ignoresUnknownTopLevelAndNestedFields() throws Exception {
        String json = """
                {
                  "response": {
                    "numFound": 42,
                    "start": 0,
                    "docs": [
                      {"pid":"sku-100","title":"Bowl","url":"https://shop.com/bowl",
                       "thumb_image":"http://img.jpg","price":9.99,"currency":"USD",
                       "variants": [{"sku":"v1","color":"red"}]}
                    ]
                  },
                  "facet_counts": {
                    "facets": [],
                    "facet_queries": {}
                  },
                  "category_map": {},
                  "did_you_mean": ["bowl set"],
                  "metadata": {"query_id":"abc123"}
                }
                """;
        stubResource(json);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        assertEquals(42L, result.total());
        assertEquals(1, result.products().size());
        assertEquals("sku-100", result.products().get(0).id());
    }

    // ── attributes ─────────────────────────────────────────────────────────────

    @Test
    void toSearchResult_populatesBrandAndDescriptionInAttributes() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img.jpg","price":19.99,"currency":"USD",
                       "brand":"Nike","description":"A cotton shirt"}
                    ]
                  }
                }
                """);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        var attrs = result.products().get(0).attributes();
        assertEquals("Nike", attrs.get("brand"));
        assertEquals("A cotton shirt", attrs.get("description"));
    }

    @Test
    void toSearchResult_omitsBlankAttributeValues() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img.jpg","price":19.99,"currency":"USD",
                       "brand":"","description":null}
                    ]
                  }
                }
                """);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        var attrs = result.products().get(0).attributes();
        assertFalse(attrs.containsKey("brand"), "Blank brand should be omitted");
        assertFalse(attrs.containsKey("description"), "Null description should be omitted");
    }

    @Test
    void toSearchResult_includesSalePriceInAttributes() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img.jpg","price":29.99,"sale_price":19.99,"currency":"USD"}
                    ]
                  }
                }
                """);

        SearchResult result = mapper.toSearchResult(resource, 0, 10);

        var attrs = result.products().get(0).attributes();
        assertEquals(0, new BigDecimal("19.99").compareTo((BigDecimal) attrs.get("sale_price")));
    }

    @Test
    void toRecommendationResult_populatesAttributes() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "thumb_image":"http://img.jpg","price":19.99,"currency":"USD",
                       "brand":"Adidas","description":"Sport shirt","sale_price":14.99}
                    ]
                  }
                }
                """);

        var result = mapper.toRecommendationResult(resource);

        var attrs = result.get(0).attributes();
        assertEquals("Adidas", attrs.get("brand"));
        assertEquals("Sport shirt", attrs.get("description"));
        assertEquals(0, new BigDecimal("14.99").compareTo((BigDecimal) attrs.get("sale_price")));
    }

    // ── toRecommendationResult ──────────────────────────────────────────────────

    @Test
    void toRecommendationResult_ignoresMetadataAndStart() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 70,
                    "start": 0,
                    "docs": [
                      {"pid":"9790","title":"Bowls Set","url":"https://shop.com/9790",
                       "thumb_image":"http://img.jpg","price":16.0,"currency":"USD"}
                    ]
                  },
                  "metadata": {
                    "widget": {"id":"4le608d9","name":"Clarity Testing","type":"mlt"},
                    "response": {"personalized_results":false}
                  }
                }
                """);

        var result = mapper.toRecommendationResult(resource);

        assertEquals(1, result.size());
        assertEquals("9790", result.get(0).id());
    }

    // ── toAutosuggestResult ─────────────────────────────────────────────────

    @Test
    void toAutosuggestResult_parsesQueryAndProductAndAttributeSuggestions() throws Exception {
        stubResource("""
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
                """);

        var result = mapper.toAutosuggestResult(resource);

        assertEquals("shi", result.originalQuery());
        assertEquals(List.of("shirts", "shipping"), result.querySuggestions());
        assertEquals(1, result.attributeSuggestions().size());
        assertEquals("brand", result.attributeSuggestions().get(0).name());
        assertEquals("Nike", result.attributeSuggestions().get(0).value());
        assertEquals("text", result.attributeSuggestions().get(0).attributeType());
        assertEquals(1, result.productSuggestions().size());
        assertEquals("p1", result.productSuggestions().get(0).id());
        assertEquals("Nike", result.productSuggestions().get(0).attributes().get("brand"));
    }

    @Test
    void toAutosuggestResult_multipleSuggestionGroups_flattenedIntoSingleResult() throws Exception {
        stubResource("""
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
                """);

        var result = mapper.toAutosuggestResult(resource);

        assertEquals("lap", result.originalQuery());
        assertEquals(List.of("laptop", "lap desk"), result.querySuggestions());
        assertEquals(1, result.attributeSuggestions().size());
        assertEquals(2, result.productSuggestions().size());
        assertEquals("p1", result.productSuggestions().get(0).id());
        assertEquals("p2", result.productSuggestions().get(1).id());
    }

    @Test
    void toAutosuggestResult_emptySuggestionGroups_returnsEmptyLists() throws Exception {
        stubResource("""
                {
                  "queryContext": {"originalQuery": "xyz"},
                  "suggestionGroups": []
                }
                """);

        var result = mapper.toAutosuggestResult(resource);

        assertEquals("xyz", result.originalQuery());
        assertTrue(result.querySuggestions().isEmpty());
        assertTrue(result.attributeSuggestions().isEmpty());
        assertTrue(result.productSuggestions().isEmpty());
    }

    @Test
    void toAutosuggestResult_nullSuggestionGroups_returnsEmptyLists() throws Exception {
        stubResource("""
                {"queryContext": {"originalQuery": "abc"}}
                """);

        var result = mapper.toAutosuggestResult(resource);

        assertEquals("abc", result.originalQuery());
        assertTrue(result.querySuggestions().isEmpty());
        assertTrue(result.attributeSuggestions().isEmpty());
        assertTrue(result.productSuggestions().isEmpty());
    }

    @Test
    void toAutosuggestResult_nullQueryContext_returnsNullOriginalQuery() throws Exception {
        stubResource("""
                {"suggestionGroups": []}
                """);

        var result = mapper.toAutosuggestResult(resource);

        assertNull(result.originalQuery());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubResource(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        when(resource.getNodeData()).thenReturn(node);
    }
}
