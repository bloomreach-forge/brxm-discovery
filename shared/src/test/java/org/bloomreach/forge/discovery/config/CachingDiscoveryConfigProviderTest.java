package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
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
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
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
class CachingDiscoveryConfigProviderTest {

    @Mock DiscoveryConfigReader configReader;
    @Mock Session session;
    @Mock Workspace workspace;
    @Mock ObservationManager observationManager;

    private DiscoveryConfig validConfig;
    private CachingDiscoveryConfigProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CachingDiscoveryConfigProvider(configReader, () -> session);
        validConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                10, "");
        lenient().when(configReader.applyEnvSysCredentials(any())).thenAnswer(returnsFirstArg());
    }

    // ── Config access ────────────────────────────────────────────────────

    @Test
    void get_callsReaderResolveWithSession() {
        when(configReader.resolve(session)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(() -> session);

        assertSame(validConfig, result);
        verify(configReader).resolve(session);
        verify(session).logout();
    }

    @Test
    void get_populatesCache_secondCallSkipsReader() {
        when(configReader.resolve(session)).thenReturn(validConfig);

        provider.get(() -> session);
        provider.get(() -> session);

        verify(configReader, times(1)).resolve(any(Session.class));
    }

    @Test
    void get_readerThrows_sessionLoggedOut() {
        when(configReader.resolve(session)).thenThrow(new ConfigurationException("no creds"));

        assertThrows(ConfigurationException.class, () -> provider.get(() -> session));
        verify(session).logout();
    }

    @Test
    void get_sessionSupplierThrows_fallsBackToDefaults() {
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(() -> { throw new RuntimeException("JCR unavailable"); });

        assertSame(validConfig, result);
        verify(configReader).readWithDefaults();
    }

    @Test
    void get_withExternalSession_usesSessionDirectly_noLogout() {
        when(configReader.resolve(session)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(session);

        assertSame(validConfig, result);
        verify(configReader).resolve(session);
        verify(session, never()).logout();
    }

    // ── Cache invalidation ───────────────────────────────────────────────

    @Test
    void invalidate_clearsCacheSoNextGetRefetches() {
        when(configReader.resolve(session)).thenReturn(validConfig);

        provider.get(() -> session);
        provider.invalidate();
        provider.get(() -> session);

        verify(configReader, times(2)).resolve(session);
    }

    // ── Concurrency ──────────────────────────────────────────────────────

    @Test
    void concurrentFirstAccess_readerResolveCalledOnlyOnce() throws Exception {
        int threadCount = 20;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger resolveCount = new AtomicInteger();
        CachingDiscoveryConfigProvider freshProvider =
                new CachingDiscoveryConfigProvider(configReader, () -> session);

        when(configReader.resolve(session)).thenAnswer(inv -> {
            resolveCount.incrementAndGet();
            Thread.sleep(5);
            return validConfig;
        });

        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Future<DiscoveryConfig>> futures = IntStream.range(0, threadCount)
                .mapToObj(i -> exec.submit(() -> {
                    try { barrier.await(); } catch (Exception ignored) {}
                    return freshProvider.get(() -> session);
                }))
                .toList();

        for (Future<DiscoveryConfig> f : futures) f.get(10, TimeUnit.SECONDS);
        exec.shutdown();

        assertEquals(1, resolveCount.get(),
                "configReader.resolve() should be called exactly once; actual: " + resolveCount.get());
    }

    @Test
    void get_envVarUpdatedAfterCachePopulated_newValueReflected() {
        DiscoveryConfig cachedBase = new DiscoveryConfig(
                "acct", "domain", null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION", 10, "");
        DiscoveryConfig withEnvCreds = new DiscoveryConfig(
                "acct", "domain", "env-api-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION", 10, "");
        when(configReader.resolve(session)).thenReturn(cachedBase);
        when(configReader.applyEnvSysCredentials(cachedBase)).thenReturn(withEnvCreds);

        DiscoveryConfig result1 = provider.get(session);
        DiscoveryConfig result2 = provider.get(session);

        assertEquals("env-api-key", result1.apiKey());
        assertEquals("env-api-key", result2.apiKey());
        verify(configReader, times(1)).resolve(session);
        verify(configReader, times(2)).applyEnvSysCredentials(cachedBase);
    }

    // ── JCR observation (embedded listener) ──────────────────────────────

    @Test
    void start_registersEventListener() throws Exception {
        lenient().when(session.getWorkspace()).thenReturn(workspace);
        lenient().when(workspace.getObservationManager()).thenReturn(observationManager);

        provider.start();

        verify(observationManager).addEventListener(
                eq(provider),
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
        when(session.getWorkspace()).thenReturn(workspace);
        when(session.isLive()).thenReturn(true);
        when(workspace.getObservationManager()).thenReturn(observationManager);
        provider.start();

        provider.close();

        verify(observationManager).removeEventListener(provider);
        verify(session).logout();
    }

    @Test
    void onEvent_invalidatesOncePerBatch() throws RepositoryException {
        when(configReader.resolve(session)).thenReturn(validConfig);
        provider.get(() -> session); // populate cache

        Event event1 = mock(Event.class);
        Event event2 = mock(Event.class);
        EventIterator events = mockEventIterator(event1, event2);

        provider.onEvent(events);
        provider.get(() -> session); // should re-fetch after invalidation

        verify(configReader, times(2)).resolve(session);
    }

    @Test
    void onEvent_emptyIterator_doesNotInvalidate() {
        when(configReader.resolve(session)).thenReturn(validConfig);
        provider.get(() -> session); // populate cache

        EventIterator events = mock(EventIterator.class);
        when(events.hasNext()).thenReturn(false);

        provider.onEvent(events);
        provider.get(() -> session); // should still use cache

        verify(configReader, times(1)).resolve(session);
    }

    private EventIterator mockEventIterator(Event... events) {
        EventIterator iter = mock(EventIterator.class);
        final int[] index = {0};
        lenient().when(iter.hasNext()).thenAnswer(inv -> index[0] < events.length);
        lenient().when(iter.nextEvent()).thenAnswer(inv -> events[index[0]++]);
        return iter;
    }
}
