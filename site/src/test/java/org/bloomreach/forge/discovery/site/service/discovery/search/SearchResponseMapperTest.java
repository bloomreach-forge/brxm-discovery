package org.bloomreach.forge.discovery.site.service.discovery.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.search.model.SearchResponse;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SearchResponseMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private SearchResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SearchResponseMapper(MAPPER);
    }

    // ── toSearchResult ──────────────────────────────────────────────────────

    @Test
    void toSearchResult_mapsProductsAndTotal() {
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

        SearchResult result = mapper.toSearchResult(json, 0, 10);

        assertEquals(200L, result.total());
        assertEquals(0, result.page());
        assertEquals(10, result.pageSize());
        assertEquals(2, result.products().size());

        var p1 = result.products().get(0);
        assertEquals("sku-001", p1.id());
        assertEquals("Shirt", p1.title());
        assertEquals("https://shop.com/shirt", p1.url());
        assertEquals("http://img1.jpg", p1.imageUrl());
        assertEquals(0, new BigDecimal("19.99").compareTo(p1.price()));
        assertEquals("USD", p1.currency());
    }

    @Test
    void toSearchResult_unknownFeedFields_flowThroughToAttributes() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"AG-10001","title":"Flakes","url":"https://site/AG-10001",
                       "thumb_image":"https://img/AG-10001.jpg","price":9.99,
                       "pet_type":"Fish & Aquatic","review_count":256,
                       "tags":["color-enhancing","tropical"]}
                    ]
                  }
                }
                """;

        var product = mapper.toSearchResult(json, 0, 10).products().get(0);

        assertEquals("Fish & Aquatic", product.attributes().get("pet_type"));
        assertEquals(256, ((Number) product.attributes().get("review_count")).intValue());
        assertFalse(product.attributes().containsKey("pid"), "pid should not be in attributes");
        assertFalse(product.attributes().containsKey("title"), "title should not be in attributes");
    }

    @Test
    void toSearchResult_standardFeedHasNoBrandOrSalePrice_noNpe() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"X1","title":"Item","url":"https://site","price":5.0}]
                  }
                }
                """;

        var product = mapper.toSearchResult(json, 0, 10).products().get(0);

        assertFalse(product.attributes().containsKey("brand"));
        assertFalse(product.attributes().containsKey("sale_price"));
    }

    @Test
    void toSearchResult_emptyDocs_returnsEmptyProductList() {
        SearchResult result = mapper.toSearchResult("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertEquals(0L, result.total());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void toSearchResult_nullResponse_returnsZeroTotalAndEmptyList() {
        SearchResult result = mapper.toSearchResult("{}", 0, 10);

        assertEquals(0L, result.total());
        assertTrue(result.products().isEmpty());
    }

    @Test
    void toSearchResult_mapsFacets() {
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

        SearchResult result = mapper.toSearchResult(json, 0, 10);

        assertTrue(result.facets().containsKey("brand"));
        var brandFacet = result.facets().get("brand");
        assertEquals("brand", brandFacet.name());
        assertEquals(2, brandFacet.value().size());
        assertEquals("Nike",   brandFacet.value().get(0).name());
        assertEquals(10L,      brandFacet.value().get(0).count());
        assertEquals("Adidas", brandFacet.value().get(1).name());
        assertEquals(5L,       brandFacet.value().get(1).count());
    }

    @Test
    void toSearchResult_mapsRangeFacet_startAndEnd() {
        String json = """
                {
                  "response": {"numFound": 20, "docs": []},
                  "facet_counts": {
                    "facets": [
                      {
                        "name": "price",
                        "type": "text_price_range",
                        "value": [
                          {"name": "0.0 TO 25.0",  "count": 42, "start": 0.0,  "end": 25.0},
                          {"name": "25.0 TO 50.0", "count": 17, "start": 25.0, "end": 50.0}
                        ]
                      }
                    ]
                  }
                }
                """;

        SearchResult result = mapper.toSearchResult(json, 0, 20);

        var priceFacet = result.facets().get("price");
        assertEquals("text_price_range", priceFacet.type());
        assertEquals(2, priceFacet.value().size());
        var bucket1 = priceFacet.value().get(0);
        assertEquals("0.0 TO 25.0", bucket1.name());
        assertEquals(42L, bucket1.count());
        assertEquals(0.0, bucket1.start());
        assertEquals(25.0, bucket1.end());
        var bucket2 = priceFacet.value().get(1);
        assertEquals(25.0, bucket2.start());
        assertEquals(50.0, bucket2.end());
    }

    @Test
    void toSearchResult_textFacetValue_startAndEndAreNull() {
        String json = """
                {
                  "response": {"numFound": 5, "docs": []},
                  "facet_counts": {
                    "facets": [{"name": "brand", "value": [{"name": "Nike", "count": 10}]}]
                  }
                }
                """;

        var value = mapper.toSearchResult(json, 0, 5).facets().get("brand").value().get(0);
        assertNull(value.start());
        assertNull(value.end());
    }

    @Test
    void toSearchResult_nullFacetCounts_returnsEmptyFacets() {
        SearchResult result = mapper.toSearchResult("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertTrue(result.facets().isEmpty());
    }

    @Test
    void toSearchResult_mapsVariantsInsideProductDoc() {
        String json = """
                {
                  "response": {
                    "numFound": 42,
                    "docs": [
                      {"pid":"sku-100","title":"Bowl","url":"https://shop.com/bowl",
                       "variants": [{"skuid":"v1","sku_color":"red"}]}
                    ]
                  }
                }
                """;

        SearchResult result = mapper.toSearchResult(json, 0, 10);

        assertEquals(1, result.products().size());
        assertEquals(1, result.products().get(0).variants().size());
        assertEquals("v1", result.products().get(0).variants().get(0).skuId());
    }

    @Test
    void toProductSummary_mapsStandardSkuFields() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Skirt","url":"https://shop.com/skirt",
                       "variants": [{
                         "skuid": "p1-grn-8",
                         "sku_color": "verdant",
                         "sku_color_group": "GREEN",
                         "sku_size": "8",
                         "sku_price": 72.99,
                         "sku_sale_price": 62.88
                       }]}
                    ]
                  }
                }
                """;

        var variant = mapper.toSearchResult(json, 0, 10).products().get(0).variants().get(0);

        assertEquals("p1-grn-8", variant.skuId());
        assertEquals("verdant",  variant.color());
        assertEquals("GREEN",    variant.colorGroup());
        assertEquals("8",        variant.size());
        assertEquals(new BigDecimal("72.99"), variant.price());
        assertEquals(new BigDecimal("62.88"), variant.salePrice());
    }

    @Test
    void toProductSummary_mapsSkuImageArrays() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Dress","url":"https://shop.com/dress",
                       "variants": [{
                         "sku_thumb_images":  ["https://img.com/thumb.jpg"],
                         "sku_large_images":  ["https://img.com/large.jpg"],
                         "sku_swatch_images": ["https://img.com/swatch.jpg"]
                       }]}
                    ]
                  }
                }
                """;

        var variant = mapper.toSearchResult(json, 0, 10).products().get(0).variants().get(0);

        assertEquals(List.of("https://img.com/thumb.jpg"),  variant.thumbnailImages());
        assertEquals(List.of("https://img.com/large.jpg"),  variant.largeImages());
        assertEquals(List.of("https://img.com/swatch.jpg"), variant.swatchImages());
    }

    @Test
    void toProductSummary_noVariants_returnsEmptyList() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"p1","title":"Hat","url":"https://shop.com/hat"}]
                  }
                }
                """;

        var product = mapper.toSearchResult(json, 0, 10).products().get(0);
        assertNotNull(product.variants());
        assertTrue(product.variants().isEmpty());
    }

    @Test
    void toSearchResult_populatesBrandAndDescriptionInAttributes() {
        String json = """
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
                """;

        var attrs = mapper.toSearchResult(json, 0, 10).products().get(0).attributes();
        assertEquals("Nike", attrs.get("brand"));
        assertEquals("A cotton shirt", attrs.get("description"));
    }

    @Test
    void toSearchResult_nullJsonValues_excludedFromAttributes() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "brand":"","description":null}
                    ]
                  }
                }
                """;

        var attrs = mapper.toSearchResult(json, 0, 10).products().get(0).attributes();
        assertTrue(attrs.containsKey("brand"), "Blank brand string passes through");
        assertEquals("", attrs.get("brand"));
        assertFalse(attrs.containsKey("description"), "Null description is excluded from attributes");
    }

    @Test
    void toSearchResult_includesSalePriceInAttributes() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [
                      {"pid":"p1","title":"Shirt","url":"https://shop.com/shirt",
                       "price":29.99,"sale_price":19.99,"currency":"USD"}
                    ]
                  }
                }
                """;

        var attrs = mapper.toSearchResult(json, 0, 10).products().get(0).attributes();
        assertEquals(19.99, ((Number) attrs.get("sale_price")).doubleValue(), 0.001);
    }

    // ── toSearchResponse / stats ────────────────────────────────────────────

    @Test
    void toSearchResponse_mapsStatsFields() {
        String json = """
                {
                  "response": {"numFound": 10, "docs": []},
                  "stats": {
                    "stats_fields": {
                      "price": {"min": 5.99, "max": 999.99, "mean": 45.23, "count": 150}
                    }
                  }
                }
                """;

        SearchResponse response = mapper.toSearchResponse(json, 0, 10);

        assertEquals(10L, response.result().total());
        var stats = response.metadata().stats();
        assertTrue(stats.containsKey("price"));
        assertEquals(5.99,   stats.get("price").min(),  0.001);
        assertEquals(999.99, stats.get("price").max(),  0.001);
        assertEquals(45.23,  stats.get("price").mean(), 0.001);
        assertEquals(150L,   stats.get("price").count());
    }

    @Test
    void toSearchResponse_multipleStatsFields_allMapped() {
        String json = """
                {
                  "response": {"numFound": 5, "docs": []},
                  "stats": {
                    "stats_fields": {
                      "price":      {"min": 1.0, "max": 100.0, "mean": 50.0, "count": 5},
                      "sale_price": {"min": 0.5, "max":  80.0, "mean": 40.0, "count": 3}
                    }
                  }
                }
                """;

        var stats = mapper.toSearchResponse(json, 0, 10).metadata().stats();
        assertEquals(2, stats.size());
        assertTrue(stats.containsKey("price"));
        assertTrue(stats.containsKey("sale_price"));
        assertEquals(0.5, stats.get("sale_price").min(), 0.001);
    }

    @Test
    void toSearchResponse_noStatsSection_returnsEmptyMap() {
        SearchResponse response = mapper.toSearchResponse("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertTrue(response.metadata().stats().isEmpty());
    }

    @Test
    void toSearchResponse_emptyStatsFields_returnsEmptyMap() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "stats": {"stats_fields": {}}
                }
                """, 0, 10);

        assertTrue(response.metadata().stats().isEmpty());
    }

    @Test
    void toSearchResponse_preservesProductsAndFacets() {
        String json = """
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
                """;

        SearchResponse response = mapper.toSearchResponse(json, 0, 10);

        assertEquals(1, response.result().products().size());
        assertEquals("p1", response.result().products().get(0).id());
        assertTrue(response.result().facets().containsKey("brand"));
        assertEquals(19.99, response.metadata().stats().get("price").min(), 0.001);
    }

    // ── toSearchResponse / did_you_mean + autoCorrectQuery ─────────────────

    @Test
    void toSearchResponse_withDidYouMean_mapsSuggestions() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "did_you_mean": ["shoes", "shoe"]
                }
                """, 0, 10);

        assertEquals(List.of("shoes", "shoe"), response.metadata().didYouMean());
    }

    @Test
    void toSearchResponse_withAutoCorrectQuery_mapsIt() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 5, "docs": []},
                  "autoCorrectQuery": "boots"
                }
                """, 0, 10);

        assertEquals("boots", response.metadata().autoCorrectQuery());
    }

    @Test
    void toSearchResponse_noSuggestions_returnsNullDidYouMeanAndAutoCorrect() {
        SearchResponse response = mapper.toSearchResponse("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertNull(response.metadata().didYouMean());
        assertNull(response.metadata().autoCorrectQuery());
    }

    @Test
    void toSearchResponse_emptyDidYouMean_returnsEmptyList() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "did_you_mean": []
                }
                """, 0, 10);

        assertNotNull(response.metadata().didYouMean());
        assertTrue(response.metadata().didYouMean().isEmpty());
    }

    // ── toSearchResponse / keywordRedirect ──────────────────────────────────

    @Test
    void toSearchResponse_withKeywordRedirect_mapsRedirectUrlAndQuery() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "keywordRedirect": {
                    "redirected_url": "https://example.com/sale",
                    "redirected_query": "sale shoes"
                  }
                }
                """, 0, 10);

        assertEquals("https://example.com/sale", response.metadata().redirectUrl());
        assertEquals("sale shoes", response.metadata().redirectQuery());
    }

    @Test
    void toSearchResponse_noKeywordRedirect_returnsNullRedirectFields() {
        SearchResponse response = mapper.toSearchResponse("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertNull(response.metadata().redirectUrl());
        assertNull(response.metadata().redirectQuery());
    }

    // ── toSearchResponse / campaign ─────────────────────────────────────────

    @Test
    void toSearchResponse_withCampaign_mapsCampaignFields() {
        SearchResponse response = mapper.toSearchResponse("""
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
                """, 0, 10);

        var campaign = response.metadata().campaign();
        assertNotNull(campaign);
        assertEquals("camp-001", campaign.id());
        assertEquals("Summer Sale", campaign.name());
        assertEquals("<p>20% off everything</p>", campaign.htmlText());
        assertEquals("https://example.com/summer-sale", campaign.bannerUrl());
        assertEquals("https://cdn.example.com/banner.jpg", campaign.imageUrl());
    }

    @Test
    void toSearchResponse_withoutCampaign_returnsNullCampaign() {
        SearchResponse response = mapper.toSearchResponse("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10);

        assertNull(response.metadata().campaign());
    }

    @Test
    void toSearchResponse_campaignWithPartialFields_mapsAvailableFields() {
        SearchResponse response = mapper.toSearchResponse("""
                {
                  "response": {"numFound": 0, "docs": []},
                  "campaign": {"id": "camp-002", "campaignName": "Flash Deal"}
                }
                """, 0, 10);

        var campaign = response.metadata().campaign();
        assertNotNull(campaign);
        assertEquals("camp-002", campaign.id());
        assertEquals("Flash Deal", campaign.name());
        assertNull(campaign.htmlText());
        assertNull(campaign.bannerUrl());
        assertNull(campaign.imageUrl());
    }

    // ── toBrowseResponse / category_map ─────────────────────────────────────

    @Test
    void toBrowseResponse_withCategoryMap_extractsCategoryName() {
        SearchResponse response = mapper.toBrowseResponse("""
                {
                  "response": {"numFound": 5, "docs": []},
                  "category_map": {"116732": "Dog Food", "99999": "Other Cat"}
                }
                """, 0, 10, "116732");

        assertEquals("Dog Food", response.metadata().categoryName());
    }

    @Test
    void toBrowseResponse_categoryIdNotInMap_returnsNullCategoryName() {
        SearchResponse response = mapper.toBrowseResponse("""
                {
                  "response": {"numFound": 3, "docs": []},
                  "category_map": {"other-cat": "Other Category"}
                }
                """, 0, 10, "dog-food");

        assertNull(response.metadata().categoryName());
    }

    @Test
    void toBrowseResponse_noCategoryMap_returnsNullCategoryName() {
        SearchResponse response = mapper.toBrowseResponse("""
                {"response": {"numFound": 0, "docs": []}}
                """, 0, 10, "116732");

        assertNull(response.metadata().categoryName());
    }

    @Test
    void toSearchResponse_categoryNameAlwaysNull() {
        SearchResponse response = mapper.toSearchResponse("""
                {"response": {"numFound": 0, "docs": []}, "category_map": {"x": "X"}}
                """, 0, 10);

        assertNull(response.metadata().categoryName());
    }
}
