package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.exception.SearchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CircuitBreakerDiscoveryTransportTest {

    /** slidingWindowSize=2, minimumNumberOfCalls=2, failureRateThreshold=50% -> opens on 2nd failing call. */
    private static final DiscoveryResilienceConfig CONFIG = new DiscoveryResilienceConfig(50, 2, 2, 60);

    @Mock DiscoveryTransport delegate;

    CircuitBreakerDiscoveryTransport transport;

    @BeforeEach
    void setUp() {
        transport = new CircuitBreakerDiscoveryTransport(delegate, CONFIG);
    }

    private static DiscoveryTransportRequest requestFor(String uri) {
        return DiscoveryTransportRequest.of(URI.create(uri), Map.of());
    }

    @Test
    void execute_delegatesAndReturnsResult_whenClosed() {
        DiscoveryTransportRequest request = requestFor("https://core.dxpapi.com/a");
        doReturn("ok").when(delegate).execute(request);

        assertEquals("ok", transport.execute(request));
        verify(delegate, times(1)).execute(request);
    }

    @Test
    void execute_opensCircuit_after5xxFailuresReachThreshold_thenFailsFastWithoutCallingDelegate() {
        DiscoveryTransportRequest request = requestFor("https://core.dxpapi.com/b");
        doThrow(new SearchException("HTTP 503", 503)).when(delegate).execute(request);

        assertThrows(SearchException.class, () -> transport.execute(request));
        assertThrows(SearchException.class, () -> transport.execute(request));
        verify(delegate, times(2)).execute(request); // both calls reached the (mocked) delegate, circuit now open

        SearchException ex = assertThrows(SearchException.class, () -> transport.execute(request));
        verify(delegate, times(2)).execute(request); // 3rd attempt failed fast - delegate NOT invoked again
        assertEquals(-1, ex.statusCode());
    }

    @Test
    void execute_4xxFailures_doNotOpenCircuit() {
        DiscoveryTransportRequest request = requestFor("https://core.dxpapi.com/c");
        doThrow(new SearchException("HTTP 400", 400)).when(delegate).execute(request);

        for (int i = 0; i < 10; i++) {
            assertThrows(SearchException.class, () -> transport.execute(request));
        }

        verify(delegate, times(10)).execute(request); // every call reached the delegate - never short-circuited
    }

    @Test
    void execute_breakerIsPerHost_openingOneHostDoesNotAffectAnother() {
        DiscoveryTransportRequest failing = requestFor("https://core.dxpapi.com/d");
        DiscoveryTransportRequest healthy = requestFor("https://pathways.dxpapi.com/d");
        doThrow(new SearchException("HTTP 503", 503)).when(delegate).execute(failing);
        doReturn("ok").when(delegate).execute(healthy);

        assertThrows(SearchException.class, () -> transport.execute(failing));
        assertThrows(SearchException.class, () -> transport.execute(failing));
        // core.dxpapi.com breaker is now open

        assertEquals("ok", transport.execute(healthy));
        verify(delegate, times(1)).execute(healthy);
    }
}
