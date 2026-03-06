package org.bloomreach.forge.discovery.site.service.discovery.search;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QueryParamParserTest {

    private DiscoveryConfig config;

    @BeforeEach
    void setUp() {
        config = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                12, "relevance"
        );
    }

    // --- toSearchQuery ---

    @Test
    void toSearchQuery_allParams_mapsCorrectly() {
        var params = paramsOf("q", "boots", "page", "2", "pageSize", "20", "sort", "price asc");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertEquals("boots", query.query());
        assertEquals(1, query.page());
        assertEquals(20, query.pageSize());
        assertEquals("price asc", query.sort());
    }

    @Test
    void toSearchQuery_missingPage_defaultsToZero() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertEquals(0, query.page());
    }

    @Test
    void toSearchQuery_missingPageSize_usesConfigDefault() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertEquals(12, query.pageSize());
    }

    @Test
    void toSearchQuery_missingSort_usesConfigDefault() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertEquals("relevance", query.sort());
    }

    @Test
    void toSearchQuery_filterParams_parsedIntoFilters() {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("filter.brand", new String[]{"Nike"});
        paramMap.put("filter.color", new String[]{"red", "blue"});
        var params = paramsFromMap(paramMap);

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertTrue(query.filters().containsKey("brand"));
        assertEquals(1, query.filters().get("brand").size());
        assertEquals("Nike", query.filters().get("brand").get(0));
        assertEquals(2, query.filters().get("color").size());
    }

    @Test
    void toSearchQuery_nonFilterParams_notIncludedInFilters() {
        var params = paramsOf("q", "shoes", "page", "1");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertTrue(query.filters().isEmpty());
    }

    @Test
    void toSearchQuery_invalidPageSize_usesConfigDefault() {
        var params = paramsOf("pageSize", "not-a-number");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, null, null, null);

        assertEquals(12, query.pageSize());
    }

    @Test
    void toSearchQuery_trackingParams_threadedThrough() {
        var params = paramsOf("q", "shoes");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config,
                "uid-token", "https://example.com/prev", "https://example.com/search");

        assertEquals("uid-token", query.brUid2());
        assertEquals("https://example.com/prev", query.refUrl());
        assertEquals("https://example.com/search", query.url());
    }

    // --- toSearchQuery with component fallbacks ---

    @Test
    void toSearchQuery_componentPageSize_usedWhenUrlParamAbsent() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 24, null, null, null, null);

        assertEquals(24, query.pageSize());
    }

    @Test
    void toSearchQuery_urlPageSizeOverridesComponentFallback() {
        var params = paramsOf("pageSize", "8");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 24, null, null, null, null);

        assertEquals(8, query.pageSize());
    }

    @Test
    void toSearchQuery_componentSort_usedWhenUrlParamAbsent() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 0, "price asc", null, null, null);

        assertEquals("price asc", query.sort());
    }

    @Test
    void toSearchQuery_urlSortOverridesComponentFallback() {
        var params = paramsOf("sort", "newest");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 0, "price asc", null, null, null);

        assertEquals("newest", query.sort());
    }

    @Test
    void toSearchQuery_zeroPageSizeFallback_fallsThroughToConfigDefault() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 0, null, null, null, null);

        assertEquals(12, query.pageSize());
    }

    @Test
    void toSearchQuery_blankSortFallback_fallsThroughToConfigDefault() {
        var params = paramsOf("q", "hat");

        SearchQuery query = QueryParamParser.toSearchQuery(params, config, 0, "", null, null, null);

        assertEquals("relevance", query.sort());
    }

    // --- toCategoryQuery ---

    @Test
    void toCategoryQuery_withCategoryId_mapsCorrectly() {
        var params = paramsOf("page", "1", "pageSize", "24");

        CategoryQuery query = QueryParamParser.toCategoryQuery("electronics", params, config, null, null, null);

        assertEquals("electronics", query.categoryId());
        assertEquals(0, query.page());
        assertEquals(24, query.pageSize());
    }

    @Test
    void toCategoryQuery_missingPagination_usesDefaults() {
        var params = paramsOf();

        CategoryQuery query = QueryParamParser.toCategoryQuery("shoes", params, config, null, null, null);

        assertEquals(0, query.page());
        assertEquals(12, query.pageSize());
        assertEquals("relevance", query.sort());
    }

    @Test
    void toCategoryQuery_withFilters_parsedIntoFilters() {
        Map<String, String[]> paramMap = new HashMap<>();
        paramMap.put("filter.size", new String[]{"10", "11"});
        var params = paramsFromMap(paramMap);

        CategoryQuery query = QueryParamParser.toCategoryQuery("shoes", params, config, null, null, null);

        assertEquals(2, query.filters().get("size").size());
    }

    @Test
    void toCategoryQuery_trackingParams_threadedThrough() {
        var params = paramsOf();

        CategoryQuery query = QueryParamParser.toCategoryQuery("apparel", params, config,
                "uid-token", "https://example.com/prev", "https://example.com/cat");

        assertEquals("uid-token", query.brUid2());
        assertEquals("https://example.com/prev", query.refUrl());
        assertEquals("https://example.com/cat", query.url());
    }

    // --- toCategoryQuery with component fallbacks ---

    @Test
    void toCategoryQuery_componentPageSize_usedWhenUrlParamAbsent() {
        var params = paramsOf("page", "1");

        CategoryQuery query = QueryParamParser.toCategoryQuery("cat-1", params, config, 24, null, null, null, null);

        assertEquals(24, query.pageSize());
    }

    @Test
    void toCategoryQuery_urlPageSizeOverridesComponentFallback() {
        var params = paramsOf("pageSize", "8");

        CategoryQuery query = QueryParamParser.toCategoryQuery("cat-1", params, config, 24, null, null, null, null);

        assertEquals(8, query.pageSize());
    }

    @Test
    void toCategoryQuery_componentSort_usedWhenUrlParamAbsent() {
        var params = paramsOf();

        CategoryQuery query = QueryParamParser.toCategoryQuery("cat-1", params, config, 0, "price asc", null, null, null);

        assertEquals("price asc", query.sort());
    }

    @Test
    void toCategoryQuery_urlSortOverridesComponentFallback() {
        var params = paramsOf("sort", "name asc");

        CategoryQuery query = QueryParamParser.toCategoryQuery("cat-1", params, config, 0, "price asc", null, null, null);

        assertEquals("name asc", query.sort());
    }

    @Test
    void toCategoryQuery_zeroPageSizeFallback_fallsThroughToConfigDefault() {
        var params = paramsOf();

        CategoryQuery query = QueryParamParser.toCategoryQuery("cat-1", params, config, 0, null, null, null, null);

        assertEquals(12, query.pageSize());
    }

    // --- helpers ---

    private QueryParamParser.RequestParamProvider paramsOf(String... keyValues) {
        Map<String, String[]> map = new HashMap<>();
        for (int i = 0; i < keyValues.length - 1; i += 2) {
            map.put(keyValues[i], new String[]{keyValues[i + 1]});
        }
        return paramsFromMap(map);
    }

    private QueryParamParser.RequestParamProvider paramsFromMap(Map<String, String[]> map) {
        return new QueryParamParser.RequestParamProvider() {
            @Override
            public String getParameter(String name) {
                String[] values = map.get(name);
                return (values != null && values.length > 0) ? values[0] : null;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Collections.unmodifiableMap(map);
            }
        };
    }
}
