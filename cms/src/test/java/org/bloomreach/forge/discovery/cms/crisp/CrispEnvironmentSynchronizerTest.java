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

import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.bloomreach.forge.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class CrispEnvironmentSynchronizerTest {

    @Mock DiscoveryConfigReader configReader;
    @Mock Session session;
    @Mock Node searchNode;
    @Mock Node pathwaysNode;
    @Mock Node autosuggestNode;

    @BeforeEach
    void stubNodes() throws RepositoryException {
        lenient().when(session.nodeExists(CrispEnvironmentSynchronizer.SEARCH_NODE)).thenReturn(true);
        lenient().when(session.nodeExists(CrispEnvironmentSynchronizer.PATHWAYS_NODE)).thenReturn(true);
        lenient().when(session.nodeExists(CrispEnvironmentSynchronizer.AUTOSUGGEST_NODE)).thenReturn(true);
        lenient().when(session.getNode(CrispEnvironmentSynchronizer.SEARCH_NODE)).thenReturn(searchNode);
        lenient().when(session.getNode(CrispEnvironmentSynchronizer.PATHWAYS_NODE)).thenReturn(pathwaysNode);
        lenient().when(session.getNode(CrispEnvironmentSynchronizer.AUTOSUGGEST_NODE)).thenReturn(autosuggestNode);
    }

    private void stubConfig(String environment) {
        DiscoveryCredentials creds = new DiscoveryCredentials(null, null, null, null, environment);
        DiscoverySettings settings = new DiscoverySettings(
                ConfigDefaults.resolveBaseUri(null, environment),
                ConfigDefaults.resolvePathwaysBaseUri(null, environment),
                ConfigDefaults.resolveAutosuggestBaseUri(null, environment),
                ConfigDefaults.DEFAULT_PAGE_SIZE,
                ConfigDefaults.DEFAULT_SORT
        );
        when(configReader.resolve(session)).thenReturn(DiscoveryConfig.of(creds, settings));
    }

    // ── sync() ───────────────────────────────────────────────────────────────

    @Test
    void sync_productionEnvironment_writesProductionUris() throws RepositoryException {
        stubConfig(ConfigDefaults.ENVIRONMENT);
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);

        boolean changed = sync.sync(session);

        assertTrue(changed);
        verify(searchNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.BASE_URI}));
        verify(pathwaysNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.PATHWAYS_BASE_URI}));
        verify(autosuggestNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.AUTOSUGGEST_BASE_URI}));
    }

    @Test
    void sync_stagingEnvironment_writesStagingUris() throws RepositoryException {
        stubConfig(ConfigDefaults.STAGING_ENVIRONMENT);
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);

        sync.sync(session);

        verify(searchNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.STAGING_BASE_URI}));
        verify(pathwaysNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.STAGING_PATHWAYS_BASE_URI}));
        verify(autosuggestNode).setProperty(eq(CrispEnvironmentSynchronizer.PROP_VALUES),
                eq(new String[]{ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI}));
    }

    @Test
    void sync_sameUrisAlreadyPresent_returnsFalse() throws RepositoryException {
        stubConfig(ConfigDefaults.STAGING_ENVIRONMENT);

        for (Node node : new Node[]{searchNode, pathwaysNode, autosuggestNode}) {
            lenient().when(node.hasProperty(CrispEnvironmentSynchronizer.PROP_VALUES)).thenReturn(true);
            Property prop = mock(Property.class);
            lenient().when(node.getProperty(CrispEnvironmentSynchronizer.PROP_VALUES)).thenReturn(prop);
            Value val = mock(Value.class);
            lenient().when(prop.getValues()).thenReturn(new Value[]{val});
        }
        // Wire each node's value to its correct staging URI
        Value searchVal = searchNode.getProperty(CrispEnvironmentSynchronizer.PROP_VALUES).getValues()[0];
        when(searchVal.getString()).thenReturn(ConfigDefaults.STAGING_BASE_URI);
        Value pathwaysVal = pathwaysNode.getProperty(CrispEnvironmentSynchronizer.PROP_VALUES).getValues()[0];
        when(pathwaysVal.getString()).thenReturn(ConfigDefaults.STAGING_PATHWAYS_BASE_URI);
        Value autosuggestVal = autosuggestNode.getProperty(CrispEnvironmentSynchronizer.PROP_VALUES).getValues()[0];
        when(autosuggestVal.getString()).thenReturn(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI);

        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);
        boolean changed = sync.sync(session);

        assertFalse(changed);
        verify(searchNode, never()).setProperty(anyString(), any(String[].class));
        verify(pathwaysNode, never()).setProperty(anyString(), any(String[].class));
        verify(autosuggestNode, never()).setProperty(anyString(), any(String[].class));
    }

    @Test
    void sync_missingSearchNode_skipsItAndReturnsChangedForOthers() throws RepositoryException {
        stubConfig(ConfigDefaults.ENVIRONMENT);
        when(session.nodeExists(CrispEnvironmentSynchronizer.SEARCH_NODE)).thenReturn(false);
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);

        boolean changed = sync.sync(session);

        assertTrue(changed);
        verify(searchNode, never()).setProperty(anyString(), any(String[].class));
        verify(pathwaysNode).setProperty(anyString(), any(String[].class));
        verify(autosuggestNode).setProperty(anyString(), any(String[].class));
    }

    @Test
    void sync_allNodesMissing_returnsFalse() throws RepositoryException {
        when(session.nodeExists(anyString())).thenReturn(false);
        when(configReader.resolve(session)).thenReturn(DiscoveryConfig.of(
                new DiscoveryCredentials(null, null, null, null, ConfigDefaults.ENVIRONMENT),
                new DiscoverySettings(ConfigDefaults.BASE_URI, ConfigDefaults.PATHWAYS_BASE_URI,
                        ConfigDefaults.AUTOSUGGEST_BASE_URI, 12, null)));
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);

        boolean changed = sync.sync(session);

        assertFalse(changed);
    }

    // ── onEvent() ────────────────────────────────────────────────────────────

    @Test
    void onEvent_withChanges_opensFreshSessionAndSyncs() throws Exception {
        Session freshSession = mock(Session.class);
        when(freshSession.nodeExists(anyString())).thenReturn(false); // no nodes - just verifies flow
        when(freshSession.isLive()).thenReturn(true);
        when(configReader.resolve(freshSession)).thenReturn(DiscoveryConfig.of(
                new DiscoveryCredentials(null, null, null, null, ConfigDefaults.ENVIRONMENT),
                new DiscoverySettings(ConfigDefaults.BASE_URI, ConfigDefaults.PATHWAYS_BASE_URI,
                        ConfigDefaults.AUTOSUGGEST_BASE_URI, 12, null)));

        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> freshSession);
        EventIterator events = mockEventIterator(2);

        sync.onEvent(events);

        verify(freshSession).isLive();
        verify(freshSession).logout();
        verify(freshSession, never()).save(); // nodes missing → no writes → no save
    }

    @Test
    void onEvent_withChangesAndAllNodesPresent_savesSession() throws Exception {
        Session freshSession = mock(Session.class);
        when(freshSession.nodeExists(CrispEnvironmentSynchronizer.SEARCH_NODE)).thenReturn(true);
        when(freshSession.nodeExists(CrispEnvironmentSynchronizer.PATHWAYS_NODE)).thenReturn(true);
        when(freshSession.nodeExists(CrispEnvironmentSynchronizer.AUTOSUGGEST_NODE)).thenReturn(true);
        when(freshSession.getNode(CrispEnvironmentSynchronizer.SEARCH_NODE)).thenReturn(searchNode);
        when(freshSession.getNode(CrispEnvironmentSynchronizer.PATHWAYS_NODE)).thenReturn(pathwaysNode);
        when(freshSession.getNode(CrispEnvironmentSynchronizer.AUTOSUGGEST_NODE)).thenReturn(autosuggestNode);
        when(freshSession.isLive()).thenReturn(true);
        when(configReader.resolve(freshSession)).thenReturn(DiscoveryConfig.of(
                new DiscoveryCredentials(null, null, null, null, ConfigDefaults.ENVIRONMENT),
                new DiscoverySettings(ConfigDefaults.BASE_URI, ConfigDefaults.PATHWAYS_BASE_URI,
                        ConfigDefaults.AUTOSUGGEST_BASE_URI, 12, null)));

        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> freshSession);
        EventIterator events = mockEventIterator(1);

        sync.onEvent(events);

        verify(freshSession).save();
        verify(freshSession).logout();
    }

    @Test
    void onEvent_emptyEventIterator_doesNothing() throws Exception {
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);
        EventIterator events = mockEventIterator(0);

        sync.onEvent(events);

        verify(configReader, never()).resolve(any());
        verify(session, never()).save();
    }

    @Test
    void onEvent_sessionSupplierThrows_doesNotPropagate() {
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(
                configReader, () -> { throw new RuntimeException("no session"); });
        EventIterator events = mockEventIterator(1);

        sync.onEvent(events); // must not throw
    }

    // ── start() / close() ────────────────────────────────────────────────────

    @Test
    void start_registersObservationListener() throws Exception {
        Workspace workspace = mock(Workspace.class);
        ObservationManager om = mock(ObservationManager.class);
        Session obsSession = mock(Session.class);
        when(obsSession.getWorkspace()).thenReturn(workspace);
        when(workspace.getObservationManager()).thenReturn(om);

        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> obsSession);
        sync.start();

        verify(om).addEventListener(eq(sync), anyInt(), anyString(), anyBoolean(),
                isNull(), any(String[].class), anyBoolean());
    }

    @Test
    void close_removesListenerAndLogsOutSession() throws Exception {
        Workspace workspace = mock(Workspace.class);
        ObservationManager om = mock(ObservationManager.class);
        Session obsSession = mock(Session.class);
        when(obsSession.getWorkspace()).thenReturn(workspace);
        when(workspace.getObservationManager()).thenReturn(om);
        when(obsSession.isLive()).thenReturn(true);

        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> obsSession);
        sync.start();
        sync.close();

        verify(om).removeEventListener(sync);
        verify(obsSession).logout();
    }

    @Test
    void close_beforeStart_doesNotThrow() {
        CrispEnvironmentSynchronizer sync = new CrispEnvironmentSynchronizer(configReader, () -> session);
        sync.close(); // must not throw
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static EventIterator mockEventIterator(int eventCount) {
        EventIterator it = mock(EventIterator.class);
        if (eventCount == 0) {
            when(it.hasNext()).thenReturn(false);
        } else {
            Boolean[] returns = new Boolean[eventCount + 1];
            for (int i = 0; i < eventCount; i++) returns[i] = true;
            returns[eventCount] = false;
            when(it.hasNext()).thenReturn(returns[0], java.util.Arrays.copyOfRange(returns, 1, returns.length));
            when(it.nextEvent()).thenReturn(mock(javax.jcr.observation.Event.class));
        }
        return it;
    }
}
