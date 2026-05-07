package org.bloomreach.forge.discovery.request;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.RangeSelection;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryRequestFactoryTest {

    private static final DiscoveryCredentials CREDENTIALS =
            new DiscoveryCredentials("acct", "domain", "api-key", "pathways-key", "PRODUCTION");

    @Test
    void search_buildsKeywordSearchSpec() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-123");
        SearchQuery query = new SearchQuery(
                "shirt", 2, 24, "sale_price asc",
                Map.of("brand", List.of("Nike", "Adidas")),
                "uid=abc", "https://ref.example", "https://site.example/search",
                "storefront", List.of("price"), "vip", "inventory:false")
                .withFields("pid,title,thumb_image,url,price");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(DiscoveryRequestFactory.CORE_PATH, request.path());
        assertEquals("acct", valueOf(request, "account_id"));
        assertEquals("domain", valueOf(request, "domain_key"));
        assertEquals("api-key", valueOf(request, "auth_key"));
        assertEquals("search", valueOf(request, "request_type"));
        assertEquals("keyword", valueOf(request, "search_type"));
        assertEquals("shirt", valueOf(request, "q"));
        assertEquals("req-123", valueOf(request, "request_id"));
        assertEquals("pid,title,thumb_image,url,price", valueOf(request, "fl"));
        assertEquals("storefront", valueOf(request, "catalog_name"));
        assertEquals("uid=abc", valueOf(request, "_br_uid_2"));
        assertEquals("48", valueOf(request, "start"));
        assertEquals("24", valueOf(request, "rows"));
        assertEquals("sale_price asc", valueOf(request, "sort"));
        assertEquals("price", valueOf(request, "stats.field"));
        assertEquals("vip", valueOf(request, "segment"));
        assertEquals("inventory:false", valueOf(request, "efq"));
        assertEquals(2, countOf(request, "fq"));
    }

    @Test
    void search_noFieldsSet_emitsNoFlParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-nofl");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(0, countOf(request, "fl"));
    }

    @Test
    void category_withFields_emitsFlParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-cat-fl");
        CategoryQuery query = new CategoryQuery("sale", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withFields("pid,title,pet_type,size");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("pid,title,pet_type,size", valueOf(request, "fl"));
    }

    @Test
    void category_buildsCategoryBrowseSpec() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-456");
        CategoryQuery query = new CategoryQuery("sale", 0, 0, null, Map.of(), "uid=xyz", null, "https://cms.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("category", valueOf(request, "search_type"));
        assertEquals("sale", valueOf(request, "q"));
        assertEquals("req-456", valueOf(request, "request_id"));
        assertEquals("0", valueOf(request, "rows"));
    }

    @Test
    void category_emptyCategoryId_sendsWildcardQ() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-cats");
        CategoryQuery query = new CategoryQuery("", 0, 0, null, Map.of(), "uid=xyz", null, "https://cms.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("*", valueOf(request, "q"));
        assertEquals("0", valueOf(request, "rows"));
    }

    @Test
    void productLookup_buildsSingleItemSpec() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-789");

        DiscoveryRequestSpec request = factory.productLookup("sku-1", "https://site.example/product",
                "pid,title,thumb_image,url,price", CREDENTIALS);

        assertEquals("req-789", valueOf(request, "request_id"));
        assertEquals("*", valueOf(request, "q"));
        assertEquals("1", valueOf(request, "rows"));
        assertEquals("pid:(sku-1)", valueOf(request, "efq"));
        assertEquals("pid,title,thumb_image,url,price", valueOf(request, "fl"));
    }

    @Test
    void autosuggest_usesStandardCredentialQueryParams() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-auto");
        AutosuggestQuery query = new AutosuggestQuery("shi", 8, null, "uid-1", "https://ref.example", "https://page.example");

        DiscoveryRequestSpec request = factory.autosuggest(query, CREDENTIALS);

        assertEquals(DiscoveryRequestFactory.AUTOSUGGEST_PATH, request.path());
        assertEquals("api-key", valueOf(request, "auth_key"));
        assertEquals("suggest", valueOf(request, "request_type"));
        assertEquals("domain", valueOf(request, "catalog_views"));
        assertEquals("uid-1", valueOf(request, "_br_uid_2"));
    }

    @Test
    void merchantWidgets_usesApiKeyQueryParamNotPathwaysAuthKey() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-widget");

        DiscoveryRequestSpec request = factory.merchantWidgets(CREDENTIALS);

        assertEquals(DiscoveryRequestFactory.WIDGETS_PATH, request.path());
        assertEquals("api-key", valueOf(request, "auth_key"));
        assertTrue(request.queryParameters().stream().noneMatch(parameter -> "pathways-key".equals(parameter.value())));
    }

    @Test
    void recommendationV2_usesHeaderStyleCredentialModelInPathSpec() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-v2");
        RecQuery query = new RecQuery("mlt", "widget-1", "prod-1", "pdp", 6, null, "brand:Acme", "https://page", "https://ref", "uid");

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals("/api/v2/widgets/item/widget-1", request.path());
        assertEquals("req-v2", valueOf(request, "request_id"));
        assertEquals("prod-1", valueOf(request, "item_ids"));
        assertEquals("pdp", valueOf(request, "context.page_type"));
        assertEquals("brand:Acme", valueOf(request, "filter"));
        assertTrue(request.queryParameters().stream().noneMatch(parameter -> parameter.name().equals("auth_key")));
    }

    // ---- facet.version --------------------------------------------------------

    @Test
    void search_alwaysSendsFacetVersionV3() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-fv");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals("3.0", valueOf(request, "facet.version"));
    }

    @Test
    void category_alwaysSendsFacetVersionV3() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-fv-cat");
        CategoryQuery query = new CategoryQuery("sale", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("3.0", valueOf(request, "facet.version"));
    }

    // ---- range filters --------------------------------------------------------

    @Test
    void search_withRangeFilters_appendsFqRangeParams() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-range");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withRangeFilters(Map.of("price", new RangeSelection(10.0, 100.0)));

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals("price:[10.0 TO 100.0]", valueOf(request, "fq"));
    }

    @Test
    void category_withRangeFilters_appendsFqRangeParams() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-cat-range");
        CategoryQuery query = new CategoryQuery("boots", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withRangeFilters(Map.of("price", new RangeSelection(20.0, 80.0)));

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("price:[20.0 TO 80.0]", valueOf(request, "fq"));
    }

    @Test
    void search_withPartialRangeFilter_startOnly_usesWildcardForEnd() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-partial");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withRangeFilters(Map.of("price", new RangeSelection(50.0, null)));

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals("price:[50.0 TO *]", valueOf(request, "fq"));
    }

    @Test
    void search_withEmptyRangeFilters_noFqParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-norange");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(0, countOf(request, "fq"));
    }

    // ---- view_id ---------------------------------------------------------------

    @Test
    void search_withViewId_sendsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-vid");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withViewId("en_US");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals("en_US", valueOf(request, "view_id"));
    }

    @Test
    void search_noViewId_omitsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-no-vid");
        SearchQuery query = new SearchQuery("shoes", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(0, countOf(request, "view_id"));
    }

    @Test
    void category_withViewId_sendsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-vid-cat");
        CategoryQuery query = new CategoryQuery("sale", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withViewId("fr_CA");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("fr_CA", valueOf(request, "view_id"));
    }

    @Test
    void category_noViewId_omitsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-no-vid-cat");
        CategoryQuery query = new CategoryQuery("sale", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals(0, countOf(request, "view_id"));
    }

    @Test
    void recommendationV2_withViewId_sendsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-vid-rec");
        RecQuery query = new RecQuery("mlt", "widget-1", "prod-1", "pdp", 6, null, null, "https://page", null, "uid")
                .withViewId("de_DE");

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals("de_DE", valueOf(request, "view_id"));
    }

    @Test
    void recommendationV2_noViewId_omitsViewIdParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-no-vid-rec");
        RecQuery query = new RecQuery("mlt", "widget-1", "prod-1", "pdp", 6, null, null, "https://page", null, "uid");

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals(0, countOf(request, "view_id"));
    }

    // ---- CategoryQuery wither methods: factory maps them to query params ------

    @Test
    void category_withSegment_appendsSegmentParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-seg");
        CategoryQuery query = new CategoryQuery("boots", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withSegment("vip");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("vip", valueOf(request, "segment"));
    }

    @Test
    void category_withEfq_appendsEfqParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-efq");
        CategoryQuery query = new CategoryQuery("boots", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withEfq("inventory:true");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("inventory:true", valueOf(request, "efq"));
    }

    @Test
    void category_withStatsFields_appendsStatsFieldParams() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-stats");
        CategoryQuery query = new CategoryQuery("boots", 0, 12, null, Map.of(), "uid", null, "https://site.example")
                .withStatsFields(List.of("price", "sale_price"));

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals(2L, countOf(request, "stats.field"));
    }

    @Test
    void category_noSegmentOrEfq_noSegmentOrEfqParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-no-seg");
        CategoryQuery query = new CategoryQuery("boots", 0, 12, null, Map.of(), "uid", null, "https://site.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals(0L, countOf(request, "segment"));
        assertEquals(0L, countOf(request, "efq"));
    }

    // ---- RecQuery backwards-compat constructors --------------------------------

    @Test
    void recQuery_v1Compat_setsOnlyWidgetIdProductIdPageTypeAndLimit() {
        RecQuery q = new RecQuery("w-1", "prod-1", "pdp", 6);

        assertEquals("w-1", q.widgetId());
        assertEquals("prod-1", q.contextProductId());
        assertEquals("pdp", q.contextPageType());
        assertEquals(6, q.limit());
    }

    @Test
    void recQuery_preOrigRefUrlCompat_populatesFieldsAndLeavesOrigRefUrlNull() {
        RecQuery q = new RecQuery("mlt", "w-2", "prod-2", "pdp", 5, "pid,title", "brand:A", "https://page", "https://ref", "uid");

        assertEquals("mlt", q.widgetType());
        assertEquals("w-2", q.widgetId());
        assertEquals("prod-2", q.contextProductId());
        assertEquals("pdp", q.contextPageType());
        assertEquals("brand:A", q.filters());
    }

    // ---- toV2WidgetType: algorithm type → family type mapping -----------------

    @Test
    void toV2WidgetType_bestseller_mapsToGlobal() {
        assertEquals("global", DiscoveryRequestFactory.toV2WidgetType("bestseller"));
    }

    @Test
    void toV2WidgetType_trending_product_mapsToGlobal() {
        assertEquals("global", DiscoveryRequestFactory.toV2WidgetType("trending_product"));
    }

    @Test
    void toV2WidgetType_jfy_mapsToPersonalized() {
        assertEquals("personalized", DiscoveryRequestFactory.toV2WidgetType("jfy"));
    }

    @Test
    void toV2WidgetType_past_purchases_mapsToPersonalized() {
        assertEquals("personalized", DiscoveryRequestFactory.toV2WidgetType("past_purchases"));
    }

    @Test
    void toV2WidgetType_recently_viewed_mapsToPersonalized() {
        assertEquals("personalized", DiscoveryRequestFactory.toV2WidgetType("recently_viewed"));
    }

    @Test
    void toV2WidgetType_co_viewed_mapsToItem() {
        assertEquals("item", DiscoveryRequestFactory.toV2WidgetType("co_viewed"));
    }

    @Test
    void toV2WidgetType_co_bought_mapsToItem() {
        assertEquals("item", DiscoveryRequestFactory.toV2WidgetType("co_bought"));
    }

    @Test
    void toV2WidgetType_rt_recs_mapsToItem() {
        assertEquals("item", DiscoveryRequestFactory.toV2WidgetType("rt_recs"));
    }

    @Test
    void toV2WidgetType_mlt_mapsToItem() {
        assertEquals("item", DiscoveryRequestFactory.toV2WidgetType("mlt"));
    }

    @Test
    void toV2WidgetType_search_mapsToKeyword() {
        assertEquals("keyword", DiscoveryRequestFactory.toV2WidgetType("search"));
    }

    @Test
    void toV2WidgetType_category_isIdentity() {
        assertEquals("category", DiscoveryRequestFactory.toV2WidgetType("category"));
    }

    @Test
    void recommendationV2_bestsellerWidget_usesGlobalPath() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-bs");
        RecQuery query = new RecQuery("bestseller", "widget-2", null, null, null, 8, null, null, "https://page", null, "uid", null, null, null);

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals("/api/v2/widgets/global/widget-2", request.path());
    }

    @Test
    void recommendationV2_pastPurchasesWidget_usesPersonalizedPath() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-pp");
        RecQuery query = new RecQuery("past_purchases", "widget-3", null, null, null, 8, null, null, "https://page", null, "uid", null, null, null);

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals("/api/v2/widgets/personalized/widget-3", request.path());
    }

    // ---- DiscoveryRequestSpec.toRelativePath -----------------------------------

    @Test
    void toRelativePath_noQueryParams_returnsPathOnly() {
        DiscoveryRequestSpec spec = DiscoveryRequestSpec.builder("/api/v1/core/").build();

        assertEquals("/api/v1/core/", spec.toRelativePath());
    }

    @Test
    void toRelativePath_withQueryParams_rendersAsQueryString() {
        DiscoveryRequestSpec spec = DiscoveryRequestSpec.builder("/api/v1/core/")
                .queryParam("q", "shoes")
                .queryParam("rows", 10)
                .build();

        assertEquals("/api/v1/core/?q=shoes&rows=10", spec.toRelativePath());
    }

    // ---- keyword widget: query param ------------------------------------------

    @Test
    void recommendationV2_withQuery_appendsQueryParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-kw");
        RecQuery query = new RecQuery("keyword", "w-kw", null, null, null, 8, null, null,
                "https://page", "https://ref", "uid", null, "summer boots", null);

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals("/api/v2/widgets/keyword/w-kw", request.path());
        assertEquals("summer boots", valueOf(request, "query"));
    }

    @Test
    void recommendationV2_nullQuery_omitsQueryParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-kw-null");
        RecQuery query = new RecQuery("keyword", "w-kw", null, null, null, 8, null, null,
                "https://page", "https://ref", "uid", null, null, null);

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals(0, countOf(request, "query"));
    }

    @Test
    void recommendationV2_blankQuery_omitsQueryParam() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-kw-blank");
        RecQuery query = new RecQuery("keyword", "w-kw", null, null, null, 8, null, null,
                "https://page", "https://ref", "uid", null, "   ", null);

        DiscoveryRequestSpec request = factory.recommendationV2(query, CREDENTIALS);

        assertEquals(0, countOf(request, "query"));
    }

    private static String valueOf(DiscoveryRequestSpec request, String name) {
        return request.queryParameters().stream()
                .filter(parameter -> parameter.name().equals(name))
                .map(DiscoveryRequestSpec.QueryParameter::value)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing query parameter: " + name));
    }

    private static long countOf(DiscoveryRequestSpec request, String name) {
        return request.queryParameters().stream()
                .filter(parameter -> parameter.name().equals(name))
                .count();
    }
}
