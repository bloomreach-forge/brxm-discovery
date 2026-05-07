package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

/**
 * JVM-lifetime config cache with JCR observation-based invalidation.
 *
 * <p>Caches the base {@link DiscoveryConfig} (from JCR + coded defaults) on first access.
 * Env/sys credential overrides are applied on every read (not cached) so environment
 * variable changes take effect without restart.
 *
 * <p>The embedded JCR listener observes {@code /hippo:configuration} for
 * {@code brxdis:discoveryConfig} node changes and calls {@link #invalidate()} on detection.
 * Call {@link #start()} after construction and {@link #close()} on shutdown.
 */
public class CachingDiscoveryConfigProvider implements DiscoveryConfigProvider, EventListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CachingDiscoveryConfigProvider.class);

    private static final int EVENT_TYPES =
            Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED
            | Event.NODE_ADDED | Event.NODE_REMOVED;
    private static final String OBSERVE_PATH = "/hippo:configuration";
    private static final String[] OBSERVE_NODE_TYPES = {"brxdis:discoveryConfig"};

    private final DiscoveryConfigReader configReader;
    private final SessionSupplier defaultSessionSupplier;
    private volatile DiscoveryConfig cachedConfig;

    private Session observationSession;
    private ObservationManager observationManager;

    public CachingDiscoveryConfigProvider(DiscoveryConfigReader configReader) {
        this(configReader, () -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException("HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    /** Seam for tests - allows injecting a custom session supplier without HippoServiceRegistry. */
    CachingDiscoveryConfigProvider(DiscoveryConfigReader configReader, SessionSupplier defaultSessionSupplier) {
        this.configReader = configReader;
        this.defaultSessionSupplier = defaultSessionSupplier;
    }

    // ── Config access ─────────────────────────────────────────────────────

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
            return currentConfig(() -> configReader.resolve(session));
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot read config via provided JCR session - falling back to env/sys. Cause: {}",
                    e.getMessage());
            return configReader.readWithDefaults();
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
            return currentBaseConfig(() -> configReader.resolve(session)).settings();
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot read settings via provided JCR session - falling back to defaults. Cause: {}",
                    e.getMessage());
            return configReader.readWithDefaults().settings();
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

    // ── JCR observation (embedded listener) ───────────────────────────────

    /** Starts JCR observation for config node changes. Idempotent - safe to call multiple times. */
    public void start() {
        if (observationSession != null) {
            return;
        }
        Session session = null;
        try {
            session = defaultSessionSupplier.get();
            ObservationManager om = session.getWorkspace().getObservationManager();
            om.addEventListener(this, EVENT_TYPES, OBSERVE_PATH, true, null, OBSERVE_NODE_TYPES, false);
            observationSession = session;
            observationManager = om;
            log.info("brxm-discovery: Registered JCR observation listener on '{}' (nodeType=brxdis:discoveryConfig)",
                    OBSERVE_PATH);
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot register JCR config observation listener - config changes will require a JVM restart. Cause: {}",
                    e.getMessage());
            if (session != null) {
                session.logout();
            }
        }
    }

    @Override
    public void close() {
        if (observationManager != null) {
            try {
                observationManager.removeEventListener(this);
                log.info("brxm-discovery: Removed JCR observation listener");
            } catch (RepositoryException e) {
                log.warn("brxm-discovery: Failed to remove JCR event listener: {}", e.getMessage());
            }
        }
        if (observationSession != null && observationSession.isLive()) {
            observationSession.logout();
        }
    }

    @Override
    public void onEvent(EventIterator events) {
        boolean changed = false;
        while (events.hasNext()) {
            events.nextEvent();
            changed = true;
        }
        if (changed) {
            log.debug("brxm-discovery: Config change detected - invalidating cache");
            invalidate();
        }
    }

    // ── Internals ─────────────────────────────────────────────────────────

    DiscoveryConfig get(SessionSupplier sessionSupplier) {
        DiscoveryConfig config = cachedConfig;
        if (config != null) {
            return configReader.applyEnvSysCredentials(config);
        }
        Session session;
        try {
            session = sessionSupplier.get();
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot open JCR session for config - falling back to environment/system properties. Cause: {}",
                    e.getMessage());
            return configReader.readWithDefaults();
        }
        try {
            return currentConfig(() -> configReader.resolve(session));
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
            log.warn("brxm-discovery: Cannot open JCR session for settings - falling back to defaults. Cause: {}",
                    e.getMessage());
            return configReader.readWithDefaults().settings();
        }
        try {
            return currentBaseConfig(() -> configReader.resolve(session)).settings();
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
        return configReader.applyEnvSysCredentials(currentBaseConfig(loader));
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
