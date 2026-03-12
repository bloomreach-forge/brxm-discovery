package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.recommendation.model.RecQuery;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryPixelTransport;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.RecommendationResult;
import org.bloomreach.forge.discovery.search.model.CategoryQuery;
import org.bloomreach.forge.discovery.search.model.SearchQuery;
import org.bloomreach.forge.discovery.search.model.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

public class DiscoveryPixelServiceImpl implements DiscoveryPixelService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPixelServiceImpl.class);

    private final DiscoveryPixelTransport client;
    private final Executor executor;

    public DiscoveryPixelServiceImpl(DiscoveryPixelTransport client) {
        this(client, ForkJoinPool.commonPool());
    }

    public DiscoveryPixelServiceImpl(DiscoveryPixelTransport client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Override
    public void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildSearchPixelPath(query, result, credentials, clientIp, flags);
        executor.execute(() -> fireQuietly(path, ctx, flags));
    }

    @Override
    public void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                  String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildCategoryPixelPath(query, result, credentials, clientIp, flags);
        executor.execute(() -> fireQuietly(path, ctx, flags));
    }

    @Override
    public void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildWidgetPixelPath(query, result, credentials, clientIp, flags);
        executor.execute(() -> fireQuietly(path, ctx, flags));
    }

    @Override
    public void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl, String url,
                                          DiscoveryCredentials credentials, String clientIp,
                                          ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String path = client.buildProductPageViewPixelPath(pid, prodName, brUid2, refUrl, url,
                credentials, clientIp, flags);
        executor.execute(() -> fireQuietly(path, ctx, flags));
    }

    private void fireQuietly(String path, ClientContext ctx, PixelFlags flags) {
        try {
            client.firePixelEvent(path, ctx, flags);
        } catch (Exception e) {
            log.warn("Discovery pixel event failed — path={}: {}", path, e.getMessage());
        }
    }
}
