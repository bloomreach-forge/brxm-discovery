package org.bloomreach.forge.discovery.site.config;

import org.bloomreach.forge.discovery.config.CachingDiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.function.Supplier;

/**
 * Site config provider that delegates to the CMS module's instance when available.
 *
 * <p>If {@link HippoServiceRegistry} has a registered {@link DiscoveryConfigProvider} (set by the
 * CMS picker module at startup), all calls are delegated to it — no extra JCR session or
 * observation listener is opened from the site.  When no CMS instance is registered (site-only
 * deployment) a local {@link CachingDiscoveryConfigProvider} is started as a fallback.
 */
public class DiscoveryConfigProviderBridge implements DiscoveryConfigProvider, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigProviderBridge.class);

    private final Supplier<DiscoveryConfigProvider> registryLookup;
    private final Supplier<CachingDiscoveryConfigProvider> fallbackFactory;

    private volatile DiscoveryConfigProvider delegate;
    private CachingDiscoveryConfigProvider fallback;

    public DiscoveryConfigProviderBridge(DiscoveryConfigReader reader) {
        this(() -> HippoServiceRegistry.getService(DiscoveryConfigProvider.class),
             () -> new CachingDiscoveryConfigProvider(reader));
    }

    /** Seam for tests — inject registry lookup and fallback factory directly. */
    DiscoveryConfigProviderBridge(Supplier<DiscoveryConfigProvider> registryLookup,
                                  Supplier<CachingDiscoveryConfigProvider> fallbackFactory) {
        this.registryLookup = registryLookup;
        this.fallbackFactory = fallbackFactory;
    }

    public void start() {
        try {
            DiscoveryConfigProvider registered = registryLookup.get();
            if (registered != null) {
                delegate = registered;
                log.info("brxm-discovery: site config provider delegating to CMS-registered instance (single JCR session)");
            } else {
                CachingDiscoveryConfigProvider own = fallbackFactory.get();
                own.start();
                fallback = own;
                delegate = own;
                log.info("brxm-discovery: no CMS config provider found in HippoServiceRegistry — started local fallback");
            }
        } catch (Exception e) {
            log.warn("brxm-discovery: config provider bridge failed to start — config reads will fail until corrected. Cause: {}",
                    e.getMessage());
        }
    }

    @Override
    public void close() {
        if (fallback != null) {
            fallback.close();
        }
    }

    @Override
    public DiscoveryConfig get() {
        return delegate.get();
    }

    @Override
    public DiscoveryConfig get(Session session) {
        return delegate.get(session);
    }

    @Override
    public DiscoverySettings settings() {
        return delegate.settings();
    }

    @Override
    public DiscoverySettings settings(Session session) {
        return delegate.settings(session);
    }

    @Override
    public void invalidate() {
        delegate.invalidate();
    }

    @Override
    public void invalidateAll() {
        delegate.invalidateAll();
    }
}
