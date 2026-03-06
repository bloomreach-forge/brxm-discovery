package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM-lifetime cache of {@link DiscoveryConfig} per config path.
 * <p>
 * Cache entries are populated on first request and remain for the lifetime of the JVM.
 * If JCR session acquisition fails (e.g. the HST session pool rejects admin credentials),
 * the provider falls back to environment variables / system properties and caches that result.
 * <p>
 * Thread-safety: {@link ConcurrentHashMap} guarantees safe concurrent reads and puts.
 * Benign double-compute on first concurrent miss is acceptable — the config is immutable.
 */
public class CachingDiscoveryConfigProvider implements DiscoveryConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(CachingDiscoveryConfigProvider.class);

    private final DiscoveryConfigResolver resolver;
    private final Repository repository;
    private final ConcurrentHashMap<String, DiscoveryConfig> cache = new ConcurrentHashMap<>();

    /** Production constructor — repository injected from the HST Spring context. */
    public CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver, Repository repository) {
        this.resolver = resolver;
        this.repository = repository;
    }

    /** Test constructor — no repository needed since tests always supply an explicit SessionSupplier. */
    CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver) {
        this(resolver, null);
    }

    @Override
    public DiscoveryConfig get(String configPath) {
        return get(configPath, () ->
                repository.login(new SimpleCredentials("system", new char[0])));
    }

    @Override
    public void invalidate(String configPath) {
        String removed = cache.remove(normalizeKey(configPath)) != null ? configPath : null;
        if (removed != null) {
            log.debug("Invalidated config cache entry for path '{}'", configPath);
        }
    }

    @Override
    public void invalidateAll() {
        cache.clear();
        log.debug("Invalidated all config cache entries");
    }

    /** Package-private seam used by tests to inject a mock session. */
    DiscoveryConfig get(String configPath, SessionSupplier sessionSupplier) {
        String key = normalizeKey(configPath);
        DiscoveryConfig cached = cache.get(key);
        if (cached != null) {
            return cached;
        }

        DiscoveryConfig config;
        if (key.isEmpty()) {
            // No JCR path — resolve from env/sys props and coded defaults only
            config = resolver.resolve(null, null);
        } else {
            Session session;
            try {
                session = sessionSupplier.get();
            } catch (Exception e) {
                // JCR admin session unavailable (e.g. HST session pool rejects system credentials).
                // Fall back to env/sys props so the component degrades gracefully rather than crashing.
                log.warn("brxm-discovery: Cannot open JCR session for config path '{}' — " +
                        "falling back to environment/system properties. Cause: {}", configPath, e.getMessage());
                config = resolver.resolve(null, null);
                cache.put(key, config);
                return config;
            }
            try {
                config = resolver.resolve(session, configPath);
            } finally {
                session.logout();
            }
        }

        cache.put(key, config);
        return config;
    }

    private static String normalizeKey(String path) {
        return (path == null || path.isBlank()) ? "" : path;
    }

    @FunctionalInterface
    interface SessionSupplier {
        Session get() throws Exception;
    }
}
