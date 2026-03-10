package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.site.service.discovery.search.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class DiscoveryPixelServiceImpl implements DiscoveryPixelService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPixelServiceImpl.class);

    private final DiscoveryClient client;
    private final Executor executor;

    public DiscoveryPixelServiceImpl(DiscoveryClient client) {
        this(client, ForkJoinPool.commonPool());
    }

    public DiscoveryPixelServiceImpl(DiscoveryClient client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Override
    public void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryConfig config,
                                String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildSearchPixelPath(query, result, config, clientIp, flags);
        executor.execute(() -> fireQuietly(path, config, ctx, flags));
    }

    @Override
    public void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryConfig config,
                                  String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildCategoryPixelPath(query, result, config, clientIp, flags);
        executor.execute(() -> fireQuietly(path, config, ctx, flags));
    }

    @Override
    public void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryConfig config,
                                String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildWidgetPixelPath(query, result, config, clientIp, flags);
        executor.execute(() -> fireQuietly(path, config, ctx, flags));
    }

    @Override
    public void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl, String url,
                                          DiscoveryConfig config, String clientIp,
                                          ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildProductPageViewPixelPath(pid, prodName, brUid2, refUrl, url, config, clientIp, flags);
        executor.execute(() -> fireQuietly(path, config, ctx, flags));
    }

    private void fireQuietly(String path, DiscoveryConfig config, ClientContext ctx, PixelFlags flags) {
        try {
            client.firePixelEvent(path, config, ctx, flags);
        } catch (Exception e) {
            log.warn("Discovery pixel event failed — path={}: {}", path, e.getMessage());
        }
    }
}
