package org.bloomreach.forge.discovery.config;

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

public class DiscoveryConfigJcrListener implements EventListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigJcrListener.class);

    private static final int EVENT_TYPES =
            Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED
            | Event.NODE_ADDED | Event.NODE_REMOVED;

    private static final String OBSERVE_PATH = "/hippo:configuration";
    private static final String[] OBSERVE_NODE_TYPES = {"brxdis:discoveryConfig"};

    private final DiscoveryConfigProvider configProvider;
    private final SessionSupplier sessionSupplier;

    private Session observationSession;
    private ObservationManager observationManager;

    public DiscoveryConfigJcrListener(DiscoveryConfigProvider configProvider) {
        this(configProvider, () -> {
            HippoRepository hippoRepo = HippoServiceRegistry.getService(HippoRepository.class);
            if (hippoRepo == null) {
                throw new IllegalStateException("HippoRepository not yet registered in HippoServiceRegistry");
            }
            return hippoRepo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    DiscoveryConfigJcrListener(DiscoveryConfigProvider configProvider, SessionSupplier sessionSupplier) {
        this.configProvider = configProvider;
        this.sessionSupplier = sessionSupplier;
    }

    public void start() {
        try {
            observationSession = sessionSupplier.get();
            observationManager = observationSession.getWorkspace().getObservationManager();
            observationManager.addEventListener(
                    this,
                    EVENT_TYPES,
                    OBSERVE_PATH,
                    true,
                    null,
                    OBSERVE_NODE_TYPES,
                    false
            );
            log.info("brxm-discovery: Registered JCR observation listener on '{}' (nodeType=brxdis:discoveryConfig)",
                    OBSERVE_PATH);
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot register JCR config observation listener — config changes will require a JVM restart until this is resolved. Cause: {}",
                    e.getMessage());
            observationSession = null;
            observationManager = null;
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
            log.debug("brxm-discovery: Config change detected — invalidating cache");
            configProvider.invalidate();
        }
    }

    @FunctionalInterface
    interface SessionSupplier {
        Session get() throws Exception;
    }
}
