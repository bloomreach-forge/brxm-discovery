package org.bloomreach.forge.discovery.site.service.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.Campaign;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResponse;
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

        RecommendationResult result = mapper.toRecommendationResult(resource);

        var attrs = result.products().get(0).attributes();
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

        RecommendationResult result = mapper.toRecommendationResult(resource);

        assertEquals(1, result.products().size());
        assertEquals("9790", result.products().get(0).id());
    }

    @Test
    void toRecommendationResult_extractsWrid() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Widget Item","url":"https://shop.com/p1",
                       "thumb_image":"http://img.jpg","price":9.99,"currency":"USD"}
                    ]
                  },
                  "metadata": {
                    "widget": {"rid": "rid-abc-123"}
                  }
                }
                """);

        RecommendationResult result = mapper.toRecommendationResult(resource);

        assertEquals("rid-abc-123", result.widgetResultId());
        assertEquals(1, result.products().size());
        assertEquals("p1", result.products().get(0).id());
    }

    @Test
    void toRecommendationResult_noMetadata_wridIsNull() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"p1","title":"Item","url":"https://shop.com/p1",
                               "thumb_image":"http://img.jpg","price":9.99,"currency":"USD"}]
                  }
                }
                """);

        RecommendationResult result = mapper.toRecommendationResult(resource);

        assertNull(result.widgetResultId());
        assertEquals(1, result.products().size());
    }

    @Test
    void toRecommendationResult_metadataWithoutRid_wridIsNull() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"p1","title":"Item","url":"https://shop.com/p1",
                               "thumb_image":"http://img.jpg","price":9.99,"currency":"USD"}]
                  },
                  "metadata": {
                    "widget": {"id": "some-widget-id", "name": "My Widget"}
                  }
                }
                """);

        RecommendationResult result = mapper.toRecommendationResult(resource);

        assertNull(result.widgetResultId(), "rid absent from widget metadata → widgetResultId must be null");
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

    // ── toSearchResponse / stats ────────────────────────────────────────────

    @Test
    void toSearchResponse_mapsStatsFields() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 10, "docs": []},
                  "stats": {
                    "stats_fields": {
                      "price": {"min": 5.99, "max": 999.99, "mean": 45.23, "count": 150}
                    }
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertEquals(10L, response.result().total());
        var stats = response.metadata().stats();
        assertTrue(stats.containsKey("price"), "Stats should contain 'price'");
        assertEquals(5.99,   stats.get("price").min(),  0.001);
        assertEquals(999.99, stats.get("price").max(),  0.001);
        assertEquals(45.23,  stats.get("price").mean(), 0.001);
        assertEquals(150L,   stats.get("price").count());
    }

    @Test
    void toSearchResponse_multipleStatsFields_allMapped() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 5, "docs": []},
                  "stats": {
                    "stats_fields": {
                      "price":      {"min": 1.0, "max": 100.0, "mean": 50.0, "count": 5},
                      "sale_price": {"min": 0.5, "max":  80.0, "mean": 40.0, "count": 3}
                    }
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        var stats = response.metadata().stats();
        assertEquals(2, stats.size());
        assertTrue(stats.containsKey("price"));
        assertTrue(stats.containsKey("sale_price"));
        assertEquals(0.5, stats.get("sale_price").min(), 0.001);
    }

    @Test
    void toSearchResponse_noStatsSection_returnsEmptyMap() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertTrue(response.metadata().stats().isEmpty(), "Stats should be empty when absent");
    }

    @Test
    void toSearchResponse_emptyStatsFields_returnsEmptyMap() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "stats": {"stats_fields": {}}
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertTrue(response.metadata().stats().isEmpty());
    }

    @Test
    void toSearchResponse_preservesProductsAndFacets() throws Exception {
        stubResource("""
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                               "thumb_image":"http://img.jpg","price":19.99,"currency":"USD"}]
                  },
                  "facet_counts": {
                    "facets": [{"name":"brand","value":[{"name":"Nike","count":1}]}]
                  },
                  "stats": {
                    "stats_fields": {"price": {"min":19.99,"max":19.99,"mean":19.99,"count":1}}
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertEquals(1, response.result().products().size());
        assertEquals("p1", response.result().products().get(0).id());
        assertTrue(response.result().facets().containsKey("brand"));
        assertEquals(19.99, response.metadata().stats().get("price").min(), 0.001);
    }

    // ── toSearchResponse / did_you_mean + autoCorrectQuery ─────────────────

    @Test
    void toSearchResponse_withDidYouMean_mapsSuggestions() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "did_you_mean": ["shoes", "shoe"]
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        var dym = response.metadata().didYouMean();
        assertNotNull(dym);
        assertEquals(List.of("shoes", "shoe"), dym);
    }

    @Test
    void toSearchResponse_withAutoCorrectQuery_mapsIt() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 5, "docs": []},
                  "autoCorrectQuery": "boots"
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertEquals("boots", response.metadata().autoCorrectQuery());
    }

    @Test
    void toSearchResponse_noSuggestions_returnsNullDidYouMeanAndAutoCorrect() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertNull(response.metadata().didYouMean());
        assertNull(response.metadata().autoCorrectQuery());
    }

    @Test
    void toSearchResponse_emptyDidYouMean_returnsEmptyList() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "did_you_mean": []
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertNotNull(response.metadata().didYouMean());
        assertTrue(response.metadata().didYouMean().isEmpty());
    }

    // ── toSearchResponse / keywordRedirect ──────────────────────────────────

    @Test
    void toSearchResponse_withKeywordRedirect_mapsRedirectUrlAndQuery() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "keywordRedirect": {
                    "redirected_url": "https://example.com/sale",
                    "redirected_query": "sale shoes"
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertEquals("https://example.com/sale", response.metadata().redirectUrl());
        assertEquals("sale shoes", response.metadata().redirectQuery());
    }

    @Test
    void toSearchResponse_noKeywordRedirect_returnsNullRedirectFields() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertNull(response.metadata().redirectUrl());
        assertNull(response.metadata().redirectQuery());
    }

    // ── toSearchResponse / campaign banner ──────────────────────────────────

    @Test
    void toSearchResponse_withCampaign_mapsCampaignFields() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "campaign": {
                    "id": "camp-001",
                    "campaignName": "Summer Sale",
                    "htmlText": "<p>20% off everything</p>",
                    "bannerUrl": "https://example.com/summer-sale",
                    "imageUrl": "https://cdn.example.com/banner.jpg"
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        var campaign = response.metadata().campaign();
        assertNotNull(campaign);
        assertEquals("camp-001", campaign.id());
        assertEquals("Summer Sale", campaign.name());
        assertEquals("<p>20% off everything</p>", campaign.htmlText());
        assertEquals("https://example.com/summer-sale", campaign.bannerUrl());
        assertEquals("https://cdn.example.com/banner.jpg", campaign.imageUrl());
    }

    @Test
    void toSearchResponse_withoutCampaign_returnsNullCampaign() throws Exception {
        stubResource("""
                {"response": {"numFound": 0, "docs": []}}
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        assertNull(response.metadata().campaign());
    }

    @Test
    void toSearchResponse_campaignWithPartialFields_mapsAvailableFields() throws Exception {
        stubResource("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "campaign": {
                    "id": "camp-002",
                    "campaignName": "Flash Deal"
                  }
                }
                """);

        SearchResponse response = mapper.toSearchResponse(resource, 0, 10);

        var campaign = response.metadata().campaign();
        assertNotNull(campaign);
        assertEquals("camp-002", campaign.id());
        assertEquals("Flash Deal", campaign.name());
        assertNull(campaign.htmlText());
        assertNull(campaign.bannerUrl());
        assertNull(campaign.imageUrl());
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void stubResource(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        when(resource.getNodeData()).thenReturn(node);
    }
}
