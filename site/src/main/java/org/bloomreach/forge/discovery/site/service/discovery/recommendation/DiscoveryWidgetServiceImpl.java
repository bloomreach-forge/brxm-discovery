package org.bloomreach.forge.discovery.site.service.discovery.recommendation;

import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.DiscoveryClient;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.service.discovery.recommendation.model.WidgetInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class DiscoveryWidgetServiceImpl implements DiscoveryWidgetService {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryWidgetServiceImpl.class);
    private static final long CACHE_TTL_SECONDS = 300;

    private final DiscoveryClient client;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public DiscoveryWidgetServiceImpl(DiscoveryClient client) {
        this.client = client;
    }

    @Override
    public List<WidgetInfo> listWidgets(DiscoveryConfig config) {
        validateAccountId(config);
        String accountId = config.accountId();
        CacheEntry entry = cache.get(accountId);
        if (entry != null && !entry.isExpired()) {
            return entry.widgets();
        }
        List<WidgetInfo> widgets = client.listWidgets(config);
        cache.put(accountId, new CacheEntry(widgets, Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
        return widgets;
    }

    @Override
    public Optional<WidgetInfo> findWidget(String widgetId, DiscoveryConfig config) {
        return listWidgets(config).stream()
                .filter(w -> w.id().equals(widgetId))
                .findFirst();
    }

    @Override
    public List<WidgetInfo> findByType(String widgetType, DiscoveryConfig config) {
        return listWidgets(config).stream()
                .filter(w -> w.type().equals(widgetType))
                .toList();
    }

    private static void validateAccountId(DiscoveryConfig config) {
        if (config.accountId() == null || config.accountId().isBlank()) {
            throw new ConfigurationException("DiscoveryConfig.accountId is required for widget listing");
        }
    }

    record CacheEntry(List<WidgetInfo> widgets, Instant expiry) {
        boolean isExpired() {
            return Instant.now().isAfter(expiry);
        }
    }
}
