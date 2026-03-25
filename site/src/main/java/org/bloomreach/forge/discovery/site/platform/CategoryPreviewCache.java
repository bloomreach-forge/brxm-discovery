package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.search.model.ProductSummary;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JVM-level TTL cache for category preview products.
 *
 * Keyed by (categoryId, count). TTL defaults to 5 minutes with ±20% jitter to
 * prevent multi-node cache stampede. Concurrent cache misses are benign — both
 * callers populate the same entry with equivalent Discovery results.
 * Expired entries are evicted passively on each {@link #put}.
 */
public class CategoryPreviewCache {

    private static final long DEFAULT_TTL_MS = 5 * 60 * 1000L;
    private static final double JITTER = 0.2;

    private final long ttlMs;
    private final ConcurrentHashMap<Key, Entry> store = new ConcurrentHashMap<>();

    public CategoryPreviewCache() {
        this(DEFAULT_TTL_MS);
    }

    /** Package-private seam — lets tests inject a short TTL without Thread.sleep. */
    CategoryPreviewCache(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public Optional<List<ProductSummary>> get(String categoryId, int count) {
        Entry e = store.get(new Key(categoryId, count));
        if (e == null || e.isExpired()) return Optional.empty();
        return Optional.of(e.products());
    }

    public void put(String categoryId, int count, List<ProductSummary> products) {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
        long jitter = (long) (ttlMs * JITTER * (ThreadLocalRandom.current().nextDouble() - 0.5) * 2);
        store.put(new Key(categoryId, count), new Entry(products, System.currentTimeMillis() + ttlMs + jitter));
    }

    private record Key(String categoryId, int count) {}

    record Entry(List<ProductSummary> products, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}
