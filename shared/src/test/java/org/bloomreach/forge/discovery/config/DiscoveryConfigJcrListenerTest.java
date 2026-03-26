package org.bloomreach.forge.discovery.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.ObservationManager;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void start_registersEventListener() throws Exception {
        listener.start();

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
    void close_removesEventListenerAndLogsOutSession() throws Exception {
        listener.start();

        listener.close();

        verify(observationManager).removeEventListener(listener);
        verify(session).logout();
    }

    @Test
    void onEvent_invalidatesOncePerBatch() throws RepositoryException {
        Event event1 = mockEvent(Event.PROPERTY_CHANGED);
        Event event2 = mockEvent(Event.NODE_ADDED);
        EventIterator events = mockEventIterator(event1, event2);

        listener.onEvent(events);

        verify(configProvider, times(1)).invalidate();
    }

    @Test
    void onEvent_emptyIterator_doesNotInvalidate() {
        EventIterator events = mock(EventIterator.class);
        when(events.hasNext()).thenReturn(false);

        listener.onEvent(events);

        verify(configProvider, never()).invalidate();
    }

    private Event mockEvent(int eventType) throws RepositoryException {
        Event event = mock(Event.class);
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
