package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class CachingDiscoveryConfigProvider implements DiscoveryConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(CachingDiscoveryConfigProvider.class);

    private final DiscoveryConfigResolver resolver;
    private final SessionSupplier defaultSessionSupplier;
    private volatile DiscoveryConfig cachedConfig;

    public CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver) {
        this(resolver, () -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException("HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    /** Seam for tests — allows injecting a custom session supplier without HippoServiceRegistry. */
    CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver, SessionSupplier defaultSessionSupplier) {
        this.resolver = resolver;
        this.defaultSessionSupplier = defaultSessionSupplier;
    }

    @Override
    public DiscoveryConfig get() {
        return get(defaultSessionSupplier);
    }

    @Override
    public DiscoveryConfig get(Session session) {
        if (session == null) {
            return get();
        }
        try {
            return currentConfig(() -> resolver.resolve(session));
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot read config via provided JCR session — falling back to env/sys. Cause: {}",
                    e.getMessage());
            return resolver.resolveDefaults();
        }
    }

    @Override
    public DiscoverySettings settings() {
        return settings(defaultSessionSupplier);
    }

    @Override
    public DiscoverySettings settings(Session session) {
        if (session == null) {
            return settings();
        }
        try {
            return currentBaseConfig(() -> resolver.resolve(session)).settings();
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot read settings via provided JCR session — falling back to defaults. Cause: {}",
                    e.getMessage());
            return resolver.resolveDefaults().settings();
        }
    }

    @Override
    public void invalidate() {
        if (cachedConfig != null) {
            cachedConfig = null;
            log.debug("Invalidated Discovery config cache");
        }
    }

    @Override
    public void invalidateAll() {
        invalidate();
    }

    DiscoveryConfig get(SessionSupplier sessionSupplier) {
        DiscoveryConfig config = cachedConfig;
        if (config != null) {
            return resolver.applyEnvSysCredentials(config);
        }
        Session session;
        try {
            session = sessionSupplier.get();
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot open JCR session for config — falling back to environment/system properties. Cause: {}",
                    e.getMessage());
            return resolver.resolveDefaults();
        }
        try {
            return currentConfig(() -> resolver.resolve(session));
        } catch (Exception e) {
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unexpected checked exception while resolving Discovery config", e);
        } finally {
            session.logout();
        }
    }

    DiscoverySettings settings(SessionSupplier sessionSupplier) {
        DiscoveryConfig config = cachedConfig;
        if (config != null) {
            return config.settings();
        }
        Session session;
        try {
            session = sessionSupplier.get();
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot open JCR session for settings — falling back to defaults. Cause: {}",
                    e.getMessage());
            return resolver.resolveDefaults().settings();
        }
        try {
            return currentBaseConfig(() -> resolver.resolve(session)).settings();
        } catch (Exception e) {
            if (e instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException("Unexpected checked exception while resolving Discovery settings", e);
        } finally {
            session.logout();
        }
    }

    private DiscoveryConfig currentConfig(ConfigLoader loader) throws Exception {
        return resolver.applyEnvSysCredentials(currentBaseConfig(loader));
    }

    private DiscoveryConfig currentBaseConfig(ConfigLoader loader) throws Exception {
        DiscoveryConfig config = cachedConfig;   // unsynchronized fast path (cache hit)
        if (config == null) {
            synchronized (this) {
                config = cachedConfig;           // re-read inside lock
                if (config == null) {
                    config = loader.load();
                    cachedConfig = config;
                }
            }
        }
        return config;
    }

    @FunctionalInterface
    interface SessionSupplier {
        Session get() throws Exception;
    }

    @FunctionalInterface
    interface ConfigLoader {
        DiscoveryConfig load() throws Exception;
    }
}
