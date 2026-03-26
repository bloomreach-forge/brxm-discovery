package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.search.model.ProductSummary;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CategoryPreviewCacheTest {

    private static ProductSummary product(String pid) {
        return new ProductSummary(pid, "Name", null, null, null, null, Map.of());
    }

    @Test
    void get_noEntry_returnsEmpty() {
        CategoryPreviewCache cache = new CategoryPreviewCache();
        assertTrue(cache.get("cat1", 4).isEmpty());
    }

    @Test
    void get_afterPut_returnsProducts() {
        CategoryPreviewCache cache = new CategoryPreviewCache();
        List<ProductSummary> products = List.of(product("p1"), product("p2"));
        cache.put("cat1", 4, products);
        Optional<List<ProductSummary>> hit = cache.get("cat1", 4);
        assertTrue(hit.isPresent());
        assertEquals(products, hit.get());
    }

    @Test
    void get_expiredEntry_returnsEmpty() {
        // ttlMs=0 → entry expires immediately
        CategoryPreviewCache cache = new CategoryPreviewCache(0L);
        cache.put("cat1", 4, List.of(product("p1")));
        assertTrue(cache.get("cat1", 4).isEmpty());
    }

    @Test
    void put_differentCounts_storedSeparately() {
        CategoryPreviewCache cache = new CategoryPreviewCache();
        List<ProductSummary> two = List.of(product("p1"), product("p2"));
        List<ProductSummary> one = List.of(product("p3"));
        cache.put("cat1", 2, two);
        cache.put("cat1", 1, one);
        assertEquals(two, cache.get("cat1", 2).orElseThrow());
        assertEquals(one, cache.get("cat1", 1).orElseThrow());
    }
}
