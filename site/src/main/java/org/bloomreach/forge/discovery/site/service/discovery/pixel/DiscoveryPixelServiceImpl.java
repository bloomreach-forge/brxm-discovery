package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.ProductSummary;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class DiscoveryPixelServiceImpl implements DiscoveryPixelService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPixelServiceImpl.class);

    private final DiscoveryClient client;
    private final Executor executor;

    public DiscoveryPixelServiceImpl(DiscoveryClient client) {
        this(client, ForkJoinPool.commonPool());
    }

    /** Package-private seam for synchronous test execution. */
    DiscoveryPixelServiceImpl(DiscoveryClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Override
    public void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryConfig config) {
        String path = client.buildSearchPixelPath(query, result, config);
        executor.execute(() -> fireQuietly(path, config));
    }

    @Override
    public void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryConfig config) {
        String path = client.buildCategoryPixelPath(query, result, config);
        executor.execute(() -> fireQuietly(path, config));
    }

    @Override
    public void fireWidgetEvent(RecQuery query, List<ProductSummary> products, DiscoveryConfig config) {
        String path = client.buildWidgetPixelPath(query, products, config);
        executor.execute(() -> fireQuietly(path, config));
    }

    private void fireQuietly(String path, DiscoveryConfig config) {
        try {
            client.firePixelEvent(path, config);
        } catch (Exception e) {
            log.warn("Discovery pixel event failed — path={}: {}", path, e.getMessage());
        }
    }
}
