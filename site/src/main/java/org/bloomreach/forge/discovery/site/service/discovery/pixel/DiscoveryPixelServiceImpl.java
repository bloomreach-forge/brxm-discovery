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
import java.util.concurrent.RejectedExecutionException;

public class DiscoveryPixelServiceImpl implements DiscoveryPixelService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPixelServiceImpl.class);

    private final DiscoveryPixelTransport client;
    private final Executor executor;

    public DiscoveryPixelServiceImpl(DiscoveryPixelTransport client, Executor executor) {
        this.client = client;
        this.executor = executor;
    }

    @Override
    public void fireSearchEvent(SearchQuery query, SearchResult result, DiscoveryCredentials credentials,
                                String title, String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        log.debug("Dispatching search pixel event [uid='{}', results={}]", query.brUid2(), result.total());
        submitQuietly(() -> {
            String path = client.buildSearchPixelPath(query, result, credentials, title, clientIp, flags);
            fireQuietly(path, ctx, flags);
        });
    }

    @Override
    public void fireCategoryEvent(CategoryQuery query, SearchResult result, DiscoveryCredentials credentials,
                                  String title, String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        log.debug("Dispatching category pixel event [uid='{}', cat='{}', results={}]",
                query.brUid2(), query.categoryId(), result.total());
        submitQuietly(() -> {
            String path = client.buildCategoryPixelPath(query, result, credentials, title, clientIp, flags);
            fireQuietly(path, ctx, flags);
        });
    }

    @Override
    public void fireWidgetEvent(RecQuery query, RecommendationResult result, DiscoveryCredentials credentials,
                                String pageType, String title, String clientIp, ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        String widgetId = result.widgetId() != null && !result.widgetId().isBlank()
                ? result.widgetId() : query.widgetId();
        log.debug("Dispatching widget pixel event [uid='{}', wid='{}']", query.brUid2(), widgetId);
        submitQuietly(() -> {
            String path = client.buildWidgetPixelPath(query, result, credentials, pageType, title, clientIp, flags);
            fireQuietly(path, ctx, flags);
        });
    }

    @Override
    public void fireProductPageViewEvent(String pid, String prodName, String brUid2, String refUrl,
                                         String origRefUrl, String url, String title,
                                         DiscoveryCredentials credentials, String clientIp,
                                         ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        log.debug("Dispatching product page-view pixel event [uid='{}', pid='{}']", brUid2, pid);
        submitQuietly(() -> {
            String path = client.buildProductPageViewPixelPath(pid, prodName, brUid2, refUrl, origRefUrl, url, title,
                    credentials, clientIp, flags);
            fireQuietly(path, ctx, flags);
        });
    }

    @Override
    public void fireDeferredEvent(DeferredPixelEvent event, DiscoveryCredentials credentials, String clientIp,
                                  ClientContext ctx, PixelFlags flags) {
        if (!flags.enabled()) return;
        log.debug("Dispatching deferred pixel event [uid='{}', group='{}', etype='{}']",
                event.brUid2(), event.group(), event.etype());
        submitQuietly(() -> {
            String path = client.buildDeferredEventPixelPath(event, credentials, clientIp, flags);
            fireQuietly(path, ctx, flags);
        });
    }

    private void submitQuietly(Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.warn("Discovery pixel event failed before send: {}", e.getMessage());
                }
            });
        } catch (RejectedExecutionException e) {
            log.warn("Discovery pixel event dropped: executor rejected task: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("Discovery pixel event submission failed: {}", e.getMessage());
        }
    }

    private void fireQuietly(String path, ClientContext ctx, PixelFlags flags) {
        try {
            client.firePixelEvent(path, ctx, flags);
        } catch (Exception e) {
            log.warn("Discovery pixel event failed: {}", e.getMessage());
        }
    }
}
