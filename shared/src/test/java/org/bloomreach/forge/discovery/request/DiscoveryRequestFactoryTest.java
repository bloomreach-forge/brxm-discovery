package org.bloomreach.forge.discovery.request;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.search.model.AutosuggestQuery;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
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
                "storefront", List.of("price"), "vip", "inventory:false");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(DiscoveryRequestFactory.CORE_PATH, request.path());
        assertEquals("acct", valueOf(request, "account_id"));
        assertEquals("domain", valueOf(request, "domain_key"));
        assertEquals("api-key", valueOf(request, "auth_key"));
        assertEquals("search", valueOf(request, "request_type"));
        assertEquals("keyword", valueOf(request, "search_type"));
        assertEquals("shirt", valueOf(request, "q"));
        assertEquals("req-123", valueOf(request, "request_id"));
        assertEquals(DiscoveryRequestFactory.DEFAULT_FIELDS, valueOf(request, "fl"));
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
    void productLookup_buildsSingleItemSpec() {
        DiscoveryRequestFactory factory = new DiscoveryRequestFactory(() -> "req-789");

        DiscoveryRequestSpec request = factory.productLookup("sku-1", "https://site.example/product", CREDENTIALS);

        assertEquals("req-789", valueOf(request, "request_id"));
        assertEquals("*", valueOf(request, "q"));
        assertEquals("1", valueOf(request, "rows"));
        assertEquals("pid:(sku-1)", valueOf(request, "efq"));
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
