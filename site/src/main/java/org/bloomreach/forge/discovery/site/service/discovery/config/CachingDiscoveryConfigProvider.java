package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JVM-lifetime cache of {@link DiscoveryConfig} per config path.
 * <p>
 * Cache entries are populated on first request and remain for the lifetime of the JVM.
 * Invalidation is triggered by {@link DiscoveryConfigJcrListener} on JCR observation events.
 * <p>
 * Thread-safety: {@link ConcurrentHashMap} guarantees safe concurrent reads and puts.
 * Benign double-compute on first concurrent miss is acceptable — the config is immutable.
 * <p>
 * <strong>Session note:</strong> uses {@code HippoServiceRegistry.getService(HippoRepository.class)}
 * to obtain a system session for JCR reads. The {@code javax.jcr.Repository} bean in the HST Spring
 * context is the pooled delivery repository which rejects arbitrary {@code system} credentials —
 * the raw {@code HippoRepository} must be used instead.
 */
public class CachingDiscoveryConfigProvider implements DiscoveryConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(CachingDiscoveryConfigProvider.class);

    private final DiscoveryConfigResolver resolver;
    private final ConcurrentHashMap<String, DiscoveryConfig> cache = new ConcurrentHashMap<>();

    /** Production constructor. */
    public CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public DiscoveryConfig get(String configPath) {
        return get(configPath, () -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException(
                        "HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    @Override
    public void invalidate(String configPath) {
        if (cache.remove(normalizeKey(configPath)) != null) {
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
                // JCR admin session unavailable (e.g. HippoRepository not yet registered,
                // or system credentials rejected). Fall back to env/sys props so the component
                // degrades gracefully rather than crashing.
                // Intentionally NOT caching — allow next request to retry JCR once it's available.
                log.warn("brxm-discovery: Cannot open JCR session for config path '{}' — " +
                        "falling back to environment/system properties. Cause: {}", configPath, e.getMessage());
                return resolver.resolve(null, null);
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
