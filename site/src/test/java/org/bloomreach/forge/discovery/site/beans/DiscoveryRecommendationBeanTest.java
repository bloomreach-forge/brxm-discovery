package org.bloomreach.forge.discovery.site.beans;

import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.DiscoveryRecommendationConfig;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Covers the JSON-parsing and property-mapping logic inside the recommendation beans
 * and DiscoveryCategoryBean without a live JCR repository.
 * Each inner subclass overrides getSingleProperty() to inject fixture data.
 */
class DiscoveryRecommendationBeanTest {

    // ── DiscoveryProductRecommendationBean.getConfig() ────────────────────

    @Test
    void productBean_getConfig_validJson_returnsConfig() {
        String json = """
                {"widgetId":"w-prod","widgetType":"item","contextProductId":"pid-1"}
                """;
        var bean = productBean(json);

        Optional<DiscoveryRecommendationConfig> cfg = bean.getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("w-prod", cfg.get().widgetId());
        assertEquals("item", cfg.get().widgetType());
        assertEquals("pid-1", cfg.get().contextProductId());
    }

    @Test
    void productBean_getConfig_nullProperty_returnsEmpty() {
        assertTrue(productBean(null).getConfig().isEmpty());
    }

    @Test
    void productBean_getConfig_blankProperty_returnsEmpty() {
        assertTrue(productBean("   ").getConfig().isEmpty());
    }

    @Test
    void productBean_getConfig_malformedJson_returnsEmpty() {
        assertTrue(productBean("{not valid json").getConfig().isEmpty());
    }

    @Test
    void productBean_getConfig_unknownFields_ignoredGracefully() {
        String json = """
                {"widgetId":"w-1","widgetType":"item","unknownFutureField":"someValue"}
                """;
        Optional<DiscoveryRecommendationConfig> cfg = productBean(json).getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("w-1", cfg.get().widgetId());
    }

    // ── DiscoveryCategoryRecommendationBean.getConfig() ───────────────────

    @Test
    void categoryRecommendationBean_getConfig_validJson_returnsConfig() {
        String json = """
                {"widgetId":"w-cat","widgetType":"category","contextCategoryId":"cat-42"}
                """;
        var bean = categoryRecommendationBean(json);

        Optional<DiscoveryRecommendationConfig> cfg = bean.getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("w-cat", cfg.get().widgetId());
        assertEquals("cat-42", cfg.get().contextCategoryId());
    }

    @Test
    void categoryRecommendationBean_getConfig_nullProperty_returnsEmpty() {
        assertTrue(categoryRecommendationBean(null).getConfig().isEmpty());
    }

    // ── DiscoveryGlobalRecommendationBean.getConfig() ─────────────────────

    @Test
    void globalBean_getConfig_validJson_returnsConfig() {
        String json = """
                {"widgetId":"w-global","widgetType":"global"}
                """;
        Optional<DiscoveryRecommendationConfig> cfg = globalBean(json).getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("w-global", cfg.get().widgetId());
        assertEquals("global", cfg.get().widgetType());
    }

    @Test
    void globalBean_getConfig_nullProperty_returnsEmpty() {
        assertTrue(globalBean(null).getConfig().isEmpty());
    }

    // ── DiscoveryKeywordRecommendationBean.getConfig() ────────────────────

    @Test
    void keywordBean_getConfig_validJson_returnsConfig() {
        String json = """
                {"widgetId":"w-kw","widgetType":"keyword","contextQuery":"winter boots","contextQueryMode":"specific"}
                """;
        Optional<DiscoveryRecommendationConfig> cfg = keywordBean(json).getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("w-kw", cfg.get().widgetId());
        assertEquals("winter boots", cfg.get().contextQuery());
        assertEquals("specific", cfg.get().contextQueryMode());
    }

    @Test
    void keywordBean_getConfig_urlMode_returnsConfig() {
        String json = """
                {"widgetId":"w-kw","widgetType":"keyword","contextQueryMode":"url"}
                """;
        Optional<DiscoveryRecommendationConfig> cfg = keywordBean(json).getConfig();

        assertTrue(cfg.isPresent());
        assertEquals("url", cfg.get().contextQueryMode());
        assertNull(cfg.get().contextQuery());
    }

    @Test
    void keywordBean_getConfig_nullProperty_returnsEmpty() {
        assertTrue(keywordBean(null).getConfig().isEmpty());
    }

    // ── DiscoveryCategoryBean.getProductPreviewCount() ────────────────────

    @Test
    void categoryBean_productPreviewCount_validInteger_returnsClamped() {
        assertEquals(3, categoryBean("3").getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_zero_returnsZero() {
        assertEquals(0, categoryBean("0").getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_aboveMax_clampedToFour() {
        assertEquals(4, categoryBean("10").getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_negative_clampedToZero() {
        assertEquals(0, categoryBean("-1").getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_null_returnsZero() {
        assertEquals(0, categoryBean(null).getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_blank_returnsZero() {
        assertEquals(0, categoryBean("  ").getProductPreviewCount());
    }

    @Test
    void categoryBean_productPreviewCount_nonNumeric_returnsZero() {
        assertEquals(0, categoryBean("abc").getProductPreviewCount());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private static DiscoveryProductRecommendationBean productBean(String configJson) {
        return new DiscoveryProductRecommendationBean() {
            @Override public String getSingleProperty(String name) { return configJson; }
        };
    }

    private static DiscoveryCategoryRecommendationBean categoryRecommendationBean(String configJson) {
        return new DiscoveryCategoryRecommendationBean() {
            @Override public String getSingleProperty(String name) { return configJson; }
        };
    }

    private static DiscoveryGlobalRecommendationBean globalBean(String configJson) {
        return new DiscoveryGlobalRecommendationBean() {
            @Override public String getSingleProperty(String name) { return configJson; }
        };
    }

    private static DiscoveryKeywordRecommendationBean keywordBean(String configJson) {
        return new DiscoveryKeywordRecommendationBean() {
            @Override public String getSingleProperty(String name) { return configJson; }
        };
    }

    private static DiscoveryCategoryBean categoryBean(String productPreviewCount) {
        return new DiscoveryCategoryBean() {
            @Override public String getSingleProperty(String name) { return productPreviewCount; }
        };
    }
}
