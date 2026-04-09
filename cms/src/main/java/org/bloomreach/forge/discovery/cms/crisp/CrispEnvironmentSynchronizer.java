/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.bloomreach.forge.discovery.cms.crisp;

import org.bloomreach.forge.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.hippoecm.repository.HippoRepository;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.SimpleCredentials;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

/**
 * Keeps the CRISP {@code crisp:propvalues} base URIs for the three Discovery
 * API resource spaces in sync with the {@code brxdis:environment} property
 * stored in the Discovery config JCR node.
 *
 * <p>Call {@link #sync(Session)} once at module startup using the existing
 * module session, then call {@link #start()} to register a JCR observation
 * listener. From that point on, any change to the {@code brxdis:discoveryConfig}
 * node type triggers a write-back of the correct base URIs into the three CRISP
 * nodes. CRISP's own {@code RefreshableRepositoryMapResourceResolverProvider}
 * detects the changed {@code crisp:propvalues} and hot-reloads the resolvers
 * without a JVM restart.
 *
 * <p>Call {@link #close()} on module shutdown.
 */
public class CrispEnvironmentSynchronizer implements EventListener, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(CrispEnvironmentSynchronizer.class);

    static final String CRISP_CONTAINER =
            "/hippo:configuration/hippo:modules/crispregistry/hippo:moduleconfig/crisp:resourceresolvercontainer";
    static final String SEARCH_NODE      = CRISP_CONTAINER + "/discoverySearchAPI";
    static final String PATHWAYS_NODE    = CRISP_CONTAINER + "/discoveryPathwaysAPI";
    static final String AUTOSUGGEST_NODE = CRISP_CONTAINER + "/discoveryAutosuggestAPI";
    static final String PROP_VALUES      = "crisp:propvalues";

    private static final int EVENT_TYPES =
            Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED
            | Event.NODE_ADDED | Event.NODE_REMOVED;
    private static final String OBSERVE_PATH = "/hippo:configuration";
    private static final String[] OBSERVE_NODE_TYPES = {"brxdis:discoveryConfig"};

    private final DiscoveryConfigReader configReader;
    private final SessionSupplier systemSessionSupplier;

    private Session observationSession;
    private ObservationManager observationManager;

    public CrispEnvironmentSynchronizer(DiscoveryConfigReader configReader) {
        this(configReader, () -> {
            HippoRepository repo = HippoServiceRegistry.getService(HippoRepository.class);
            if (repo == null) {
                throw new IllegalStateException("HippoRepository not yet registered in HippoServiceRegistry");
            }
            return repo.login(new SimpleCredentials("system", new char[0]));
        });
    }

    /** Package-private seam for tests. */
    CrispEnvironmentSynchronizer(DiscoveryConfigReader configReader, SessionSupplier systemSessionSupplier) {
        this.configReader = configReader;
        this.systemSessionSupplier = systemSessionSupplier;
    }

    /**
     * Registers the JCR observation listener for Discovery config changes.
     * Must be called after the initial {@link #sync(Session)} so startup writes
     * are committed before asynchronous events can arrive.
     */
    public void start() {
        try {
            observationSession = systemSessionSupplier.get();
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
            log.info("brxm-discovery: CrispEnvironmentSynchronizer registered JCR listener on '{}'", OBSERVE_PATH);
        } catch (Exception e) {
            log.warn("brxm-discovery: Cannot register CRISP environment sync listener — "
                     + "environment changes require a JVM restart. Cause: {}", e.getMessage());
            observationSession = null;
            observationManager = null;
        }
    }

    /**
     * Writes the correct {@code crisp:propvalues} base URIs for the three
     * Discovery API CRISP nodes, derived from the current environment setting
     * in the Discovery config JCR node.
     *
     * <p>Nodes that do not exist in the repository are skipped silently.
     *
     * @param session writable JCR session; caller is responsible for saving
     * @return {@code true} if at least one property was written (caller should save)
     */
    public boolean sync(Session session) {
        DiscoverySettings settings = configReader.resolve(session).settings();
        boolean changed = false;
        changed |= writeUri(session, SEARCH_NODE,      settings.baseUri());
        changed |= writeUri(session, PATHWAYS_NODE,    settings.pathwaysBaseUri());
        changed |= writeUri(session, AUTOSUGGEST_NODE, settings.autosuggestBaseUri());
        if (changed) {
            log.info("brxm-discovery: CRISP environment URIs synced (search={}, pathways={}, autosuggest={})",
                    settings.baseUri(), settings.pathwaysBaseUri(), settings.autosuggestBaseUri());
        }
        return changed;
    }

    @Override
    public void onEvent(EventIterator events) {
        boolean hasEvents = false;
        while (events.hasNext()) {
            events.nextEvent();
            hasEvents = true;
        }
        if (!hasEvents) {
            return;
        }
        log.debug("brxm-discovery: Discovery config change detected — re-syncing CRISP environment URIs");
        Session session = null;
        try {
            session = systemSessionSupplier.get();
            if (sync(session)) {
                session.save();
            }
        } catch (Exception e) {
            log.warn("brxm-discovery: Failed to re-sync CRISP environment URIs: {}", e.getMessage());
        } finally {
            if (session != null && session.isLive()) {
                session.logout();
            }
        }
    }

    @Override
    public void close() {
        if (observationManager != null) {
            try {
                observationManager.removeEventListener(this);
                log.info("brxm-discovery: CrispEnvironmentSynchronizer removed JCR listener");
            } catch (RepositoryException e) {
                log.warn("brxm-discovery: Failed to remove CRISP environment sync listener: {}", e.getMessage());
            }
        }
        if (observationSession != null && observationSession.isLive()) {
            observationSession.logout();
        }
    }

    private boolean writeUri(Session session, String nodePath, String uri) {
        try {
            if (!session.nodeExists(nodePath)) {
                log.debug("brxm-discovery: CRISP node '{}' not found — skipping URI sync", nodePath);
                return false;
            }
            Node node = session.getNode(nodePath);
            if (node.hasProperty(PROP_VALUES)) {
                Value[] current = node.getProperty(PROP_VALUES).getValues();
                if (current.length == 1 && uri.equals(current[0].getString())) {
                    return false;
                }
            }
            node.setProperty(PROP_VALUES, new String[]{uri});
            return true;
        } catch (RepositoryException e) {
            log.warn("brxm-discovery: Failed to write URI to CRISP node '{}': {}", nodePath, e.getMessage());
            return false;
        }
    }

    @FunctionalInterface
    interface SessionSupplier {
        Session get() throws Exception;
    }
}
