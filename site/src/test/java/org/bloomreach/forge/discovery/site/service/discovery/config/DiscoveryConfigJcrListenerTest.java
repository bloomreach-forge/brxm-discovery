package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryConfigJcrListenerTest {

    @Mock DiscoveryConfigProvider configProvider;
    @Mock Session session;
    @Mock Workspace workspace;
    @Mock ObservationManager observationManager;

    private DiscoveryConfigJcrListener listener;

    @BeforeEach
    void setUp() throws RepositoryException {
        lenient().when(session.getWorkspace()).thenReturn(workspace);
        lenient().when(session.isLive()).thenReturn(true);
        lenient().when(workspace.getObservationManager()).thenReturn(observationManager);
        listener = new DiscoveryConfigJcrListener(configProvider, () -> session);
    }

    @Test
    void afterPropertiesSet_registersEventListener() throws Exception {
        listener.afterPropertiesSet();

        verify(observationManager).addEventListener(
                eq(listener),
                anyInt(),
                anyString(),
                anyBoolean(),
                isNull(),
                argThat(types -> types != null
                        && types.length == 1
                        && "brxdis:discoveryConfig".equals(types[0])),
                eq(false));
    }

    @Test
    void destroy_removesEventListener() throws Exception {
        listener.afterPropertiesSet();
        listener.destroy();

        verify(observationManager).removeEventListener(listener);
    }

    @Test
    void destroy_logsOutSession() throws Exception {
        listener.afterPropertiesSet();
        listener.destroy();

        verify(session).logout();
    }

    @Test
    void onEvent_propertyChanged_invalidatesConfigPath() throws RepositoryException {
        String eventPath = "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig/brxdis:accountId";
        String expectedConfigPath = "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig";

        EventIterator events = mockEvents(eventPath, Event.PROPERTY_CHANGED);

        listener.onEvent(events);

        verify(configProvider).invalidate(expectedConfigPath);
    }

    @Test
    void onEvent_nodeAdded_invalidatesConfigPath() throws RepositoryException {
        String eventPath = "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig";

        EventIterator events = mockEvents(eventPath, Event.NODE_ADDED);

        listener.onEvent(events);

        verify(configProvider).invalidate(expectedParentOrSelf(eventPath));
    }

    @Test
    void onEvent_nodeRemoved_invalidatesConfigPath() throws RepositoryException {
        String eventPath = "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig";

        EventIterator events = mockEvents(eventPath, Event.NODE_REMOVED);

        listener.onEvent(events);

        verify(configProvider).invalidate(expectedParentOrSelf(eventPath));
    }

    @Test
    void onEvent_multipleEvents_invalidatesOncePerDistinctPath() throws RepositoryException {
        String configPath = "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig";
        String prop1 = configPath + "/brxdis:accountId";
        String prop2 = configPath + "/brxdis:domainKey";

        Event event1 = mockEvent(prop1, Event.PROPERTY_CHANGED);
        Event event2 = mockEvent(prop2, Event.PROPERTY_CHANGED);
        EventIterator events = mockEventIterator(event1, event2);

        listener.onEvent(events);

        // Both props strip to same config path — should invalidate once
        verify(configProvider, times(1)).invalidate(configPath);
    }

    @Test
    void onEvent_repositoryException_doesNotPropagate() throws RepositoryException {
        Event badEvent = mock(Event.class);
        when(badEvent.getPath()).thenThrow(new RepositoryException("broken"));
        EventIterator events = mockEventIterator(badEvent);

        assertDoesNotThrow(() -> listener.onEvent(events));
    }

    @Test
    void toConfigPath_propertyEvent_stripsToParent() {
        assertEquals(
                "/a/b/config",
                DiscoveryConfigJcrListener.toConfigPath(
                        "/a/b/config/brxdis:accountId", Event.PROPERTY_CHANGED));
    }

    @Test
    void toConfigPath_nodeEvent_returnsSelf() {
        assertEquals(
                "/a/b/config",
                DiscoveryConfigJcrListener.toConfigPath("/a/b/config", Event.NODE_ADDED));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private String expectedParentOrSelf(String path) {
        // NODE events: config path IS the event path
        return path;
    }

    private EventIterator mockEvents(String path, int eventType) throws RepositoryException {
        Event event = mockEvent(path, eventType);
        return mockEventIterator(event);
    }

    private Event mockEvent(String path, int eventType) throws RepositoryException {
        Event event = mock(Event.class);
        lenient().when(event.getPath()).thenReturn(path);
        lenient().when(event.getType()).thenReturn(eventType);
        return event;
    }

    private EventIterator mockEventIterator(Event... events) {
        EventIterator iter = mock(EventIterator.class);
        final int[] index = {0};
        lenient().when(iter.hasNext()).thenAnswer(inv -> index[0] < events.length);
        lenient().when(iter.nextEvent()).thenAnswer(inv -> events[index[0]++]);
        return iter;
    }
}
