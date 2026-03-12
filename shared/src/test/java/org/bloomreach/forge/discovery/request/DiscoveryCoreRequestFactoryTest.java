package org.bloomreach.forge.discovery.request;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryCoreRequestFactoryTest {

    private static final DiscoveryCredentials CREDENTIALS =
            new DiscoveryCredentials("acct", "domain", "api-key", null, "PRODUCTION");

    @Test
    void search_buildsKeywordSearchSpec() {
        DiscoveryCoreRequestFactory factory = new DiscoveryCoreRequestFactory(() -> "req-123");
        SearchQuery query = new SearchQuery(
                "shirt", 2, 24, "sale_price asc",
                Map.of("brand", List.of("Nike", "Adidas")),
                "uid=abc", "https://ref.example", "https://site.example/search",
                "storefront", List.of("price"), "vip", "inventory:false");

        DiscoveryRequestSpec request = factory.search(query, CREDENTIALS);

        assertEquals(DiscoveryCoreRequestFactory.CORE_PATH, request.path());
        assertEquals("acct", valueOf(request, "account_id"));
        assertEquals("domain", valueOf(request, "domain_key"));
        assertEquals("api-key", valueOf(request, "auth_key"));
        assertEquals("search", valueOf(request, "request_type"));
        assertEquals("keyword", valueOf(request, "search_type"));
        assertEquals("shirt", valueOf(request, "q"));
        assertEquals("req-123", valueOf(request, "request_id"));
        assertEquals(DiscoveryCoreRequestFactory.DEFAULT_FIELDS, valueOf(request, "fl"));
        assertEquals("storefront", valueOf(request, "catalog_name"));
        assertEquals("uid=abc", valueOf(request, "_br_uid_2"));
        assertEquals("https://ref.example", valueOf(request, "ref_url"));
        assertEquals("https://site.example/search", valueOf(request, "url"));
        assertEquals("48", valueOf(request, "start"));
        assertEquals("24", valueOf(request, "rows"));
        assertEquals("sale_price asc", valueOf(request, "sort"));
        assertEquals("price", valueOf(request, "stats.field"));
        assertEquals("vip", valueOf(request, "segment"));
        assertEquals("inventory:false", valueOf(request, "efq"));
        assertEquals(2, countOf(request, "fq"));
        assertTrue(request.queryParameters().contains(new DiscoveryRequestSpec.QueryParameter("fq", "brand:\"Nike\"")));
        assertTrue(request.queryParameters().contains(new DiscoveryRequestSpec.QueryParameter("fq", "brand:\"Adidas\"")));
    }

    @Test
    void category_buildsCategoryBrowseSpec() {
        DiscoveryCoreRequestFactory factory = new DiscoveryCoreRequestFactory(() -> "req-456");
        CategoryQuery query = new CategoryQuery("sale", 0, 0, null, Map.of(), "uid=xyz", null, "https://cms.example");

        DiscoveryRequestSpec request = factory.category(query, CREDENTIALS);

        assertEquals("category", valueOf(request, "search_type"));
        assertEquals("sale", valueOf(request, "q"));
        assertEquals("req-456", valueOf(request, "request_id"));
        assertEquals("0", valueOf(request, "start"));
        assertEquals("0", valueOf(request, "rows"));
        assertEquals("uid=xyz", valueOf(request, "_br_uid_2"));
        assertEquals("https://cms.example", valueOf(request, "url"));
    }

    @Test
    void productLookup_buildsSingleItemSpec() {
        DiscoveryCoreRequestFactory factory = new DiscoveryCoreRequestFactory(() -> "req-789");

        DiscoveryRequestSpec request = factory.productLookup("sku-1", "https://site.example/product", CREDENTIALS);

        assertEquals("req-789", valueOf(request, "request_id"));
        assertEquals("*", valueOf(request, "q"));
        assertEquals("1", valueOf(request, "rows"));
        assertEquals("pid:(sku-1)", valueOf(request, "efq"));
        assertEquals("https://site.example/product", valueOf(request, "url"));
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
