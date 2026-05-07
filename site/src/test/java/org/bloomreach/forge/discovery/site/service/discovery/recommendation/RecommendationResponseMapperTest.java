package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationResponseMapperTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RecommendationResponseMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new RecommendationResponseMapper(MAPPER);
    }

    // ── toRecommendationResult ──────────────────────────────────────────────

    @Test
    void toRecommendationResult_populatesAttributes() {
        String json = """
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
                """;

        RecommendationResult result = mapper.toRecommendationResult(json);

        var attrs = result.products().get(0).attributes();
        assertEquals("Adidas", attrs.get("brand"));
        assertEquals("Sport shirt", attrs.get("description"));
        assertEquals(14.99, ((Number) attrs.get("sale_price")).doubleValue(), 0.001);
    }

    @Test
    void toRecommendationResult_ignoresMetadataAndStart() {
        String json = """
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
                """;

        RecommendationResult result = mapper.toRecommendationResult(json);

        assertEquals(1, result.products().size());
        assertEquals("9790", result.products().get(0).id());
    }

    @Test
    void toRecommendationResult_extractsWrid() {
        String json = """
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
                """;

        RecommendationResult result = mapper.toRecommendationResult(json);

        assertEquals("rid-abc-123", result.widgetResultId());
        assertEquals(1, result.products().size());
        assertEquals("p1", result.products().get(0).id());
    }

    @Test
    void toRecommendationResult_noMetadata_wridIsNull() {
        String json = """
                {
                  "response": {
                    "numFound": 1,
                    "docs": [{"pid":"p1","title":"Item","url":"https://shop.com/p1",
                               "thumb_image":"http://img.jpg","price":9.99,"currency":"USD"}]
                  }
                }
                """;

        RecommendationResult result = mapper.toRecommendationResult(json);

        assertNull(result.widgetResultId());
        assertEquals(1, result.products().size());
    }

    @Test
    void toRecommendationResult_metadataWithoutRid_wridIsNull() {
        String json = """
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
                """;

        assertNull(mapper.toRecommendationResult(json).widgetResultId());
    }

    // ── toWidgetTypeMap ─────────────────────────────────────────────────────

    @Test
    void toWidgetTypeMap_parsesWidgetList() {
        String json = """
                {
                  "response": {
                    "widgets": [
                      {"id": "w1", "type": "item"},
                      {"id": "w2", "type": "keyword"}
                    ]
                  }
                }
                """;

        Map<String, String> map = mapper.toWidgetTypeMap(json);

        assertEquals(2, map.size());
        assertEquals("item",    map.get("w1"));
        assertEquals("keyword", map.get("w2"));
    }

    @Test
    void toWidgetTypeMap_emptyWidgets_returnsEmptyMap() {
        assertEquals(Map.of(), mapper.toWidgetTypeMap("""
                {"response": {"widgets": []}}
                """));
    }

    @Test
    void toWidgetTypeMap_missingResponseWidgets_returnsEmptyMap() {
        assertEquals(Map.of(), mapper.toWidgetTypeMap("{}"));
    }
}
