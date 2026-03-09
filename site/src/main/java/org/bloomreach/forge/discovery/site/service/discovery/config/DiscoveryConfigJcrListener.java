package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;
import java.util.HashSet;
import java.util.Set;

/**
 * JCR observation listener that invalidates {@link DiscoveryConfigProvider} cache entries
 * when the Discovery configuration node or its properties change in the CMS.
 * <p>
 * Registered as a Spring bean with {@code InitializingBean}/{@code DisposableBean} lifecycle.
 * Listens to the {@code /hippo:configuration} subtree for property and node events scoped
 * to {@code brxdis:discoveryConfig} nodes — ignoring all other CMS saves.
 * <p>
 * Registration is non-fatal: if the observation session cannot be obtained at startup
 * (e.g. JVM startup ordering, HippoRepository not yet available), a WARN is logged and
 * the plugin degrades gracefully — config changes require a JVM restart until the listener
 * successfully registers.
 * <p>
 * <strong>Session note:</strong> uses {@code HippoServiceRegistry.getService(HippoRepository.class)}
 * to obtain a system session. The {@code javax.jcr.Repository} bean in the HST Spring context is the
 * pooled delivery repository ({@code LazyMultipleRepositoryImpl}) which only accepts pre-configured
 * credential pools and rejects arbitrary {@code system} credentials. The raw {@code HippoRepository}
 * bypasses the pool and authenticates via the Jackrabbit module security chain directly.
 */
public class DiscoveryConfigJcrListener implements EventListener, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigJcrListener.class);

    /** Listen to property changes and node add/remove under config nodes. */
    private static final int EVENT_TYPES =
            Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED
            | Event.NODE_ADDED | Event.NODE_REMOVED;

    /** Root path to observe — covers all possible config node locations. */
    private static final String OBSERVE_PATH = "/hippo:configuration";

    /**
     * Node type filter — restricts observation to events where the associated node
     * is of type {@code brxdis:discoveryConfig}. This dramatically reduces noise from
     * unrelated CMS saves (HST config, security, translations, etc.).
     */
    private static final String[] OBSERVE_NODE_TYPES = {"brxdis:discoveryConfig"};

    private final DiscoveryConfigProvider configProvider;
    private final SessionSupplier sessionSupplier;

    private Session observationSession;
    private ObservationManager observationManager;

    /**
     * Production constructor — obtains a system session via {@link HippoServiceRegistry}
     * at initialization time (not at construction time, so ordering is not a concern).
     */
    public DiscoveryConfigJcrListener(DiscoveryConfigProvider configProvider) {
        this(configProvider, () -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException(
                        "HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    /** Test constructor — inject a mock session supplier. */
    DiscoveryConfigJcrListener(DiscoveryConfigProvider configProvider,
                               SessionSupplier sessionSupplier) {
        this.configProvider = configProvider;
        this.sessionSupplier = sessionSupplier;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            observationSession = sessionSupplier.get();
            observationManager = observationSession.getWorkspace().getObservationManager();
            observationManager.addEventListener(
                    this,
                    EVENT_TYPES,
                    OBSERVE_PATH,
                    true,               // isDeep
                    null,               // uuid filter
                    OBSERVE_NODE_TYPES, // narrow to brxdis:discoveryConfig nodes only
                    false               // noLocal
            );
            log.info("brxm-discovery: Registered JCR observation listener on '{}' " +
                    "(nodeType=brxdis:discoveryConfig)", OBSERVE_PATH);
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot register JCR config observation listener — " +
                    "config changes will require a JVM restart until this is resolved. " +
                    "Cause: {}", e.getMessage());
            observationSession = null;
            observationManager = null;
        }
    }

    @Override
    public void destroy() {
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
        Set<String> invalidated = new HashSet<>();
        while (events.hasNext()) {
            try {
                Event event = events.nextEvent();
                String configPath = toConfigPath(event.getPath(), event.getType());
                if (configPath != null && invalidated.add(configPath)) {
                    log.debug("brxm-discovery: Config change detected at '{}' — invalidating cache", configPath);
                    configProvider.invalidate(configPath);
                }
            } catch (RepositoryException e) {
                log.warn("brxm-discovery: Error processing JCR event: {}", e.getMessage());
            }
        }
    }

    /**
     * Extracts the config node path from an event path.
     * Property events have paths like {@code /path/to/configNode/brxdis:accountId} — strip to parent.
     * Node events have paths like {@code /path/to/configNode} — return as-is.
     */
    static String toConfigPath(String eventPath, int eventType) {
        if (eventPath == null || eventPath.isBlank()) return null;
        boolean isPropertyEvent = (eventType & (Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED)) != 0;
        if (isPropertyEvent) {
            int lastSlash = eventPath.lastIndexOf('/');
            return lastSlash > 0 ? eventPath.substring(0, lastSlash) : eventPath;
        }
        return eventPath;
    }

    @FunctionalInterface
    interface SessionSupplier {
        Session get() throws Exception;
    }
}
