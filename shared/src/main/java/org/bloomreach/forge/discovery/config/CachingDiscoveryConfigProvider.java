package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

public class CachingDiscoveryConfigProvider implements DiscoveryConfigProvider {

    private static final Logger log = LoggerFactory.getLogger(CachingDiscoveryConfigProvider.class);

    private final DiscoveryConfigResolver resolver;
    private volatile DiscoveryConfig cachedConfig;

    public CachingDiscoveryConfigProvider(DiscoveryConfigResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public DiscoveryConfig get() {
        return get(() -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException("HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
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

    private DiscoveryConfig currentConfig(ConfigLoader loader) throws Exception {
        DiscoveryConfig config = cachedConfig;
        if (config == null) {
            config = loader.load();
            cachedConfig = config;
        }
        return resolver.applyEnvSysCredentials(config);
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
