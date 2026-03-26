package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingDiscoveryConfigProviderTest {

    @Mock DiscoveryConfigResolver resolver;
    @Mock Session session;

    private DiscoveryConfig validConfig;
    private CachingDiscoveryConfigProvider provider;

    @BeforeEach
    void setUp() {
        provider = new CachingDiscoveryConfigProvider(resolver);
        validConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                10, "");
        lenient().when(resolver.applyEnvSysCredentials(any())).thenAnswer(returnsFirstArg());
    }

    @Test
    void get_callsResolverWithAdminSession() {
        when(resolver.resolve(session)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(() -> session);

        assertSame(validConfig, result);
        verify(resolver).resolve(session);
        verify(session).logout();
    }

    @Test
    void get_populatesCache_secondCallSkipsResolver() {
        when(resolver.resolve(session)).thenReturn(validConfig);

        provider.get(() -> session);
        provider.get(() -> session);

        verify(resolver, times(1)).resolve(any(Session.class));
    }

    @Test
    void get_resolverThrows_sessionLoggedOut() {
        when(resolver.resolve(session)).thenThrow(new ConfigurationException("no creds"));

        assertThrows(ConfigurationException.class, () -> provider.get(() -> session));
        verify(session).logout();
    }

    @Test
    void get_sessionSupplierThrows_fallsBackToDefaults() {
        when(resolver.resolveDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(() -> { throw new RuntimeException("JCR unavailable"); });

        assertSame(validConfig, result);
        verify(resolver).resolveDefaults();
    }

    @Test
    void get_withExternalSession_usesSessionDirectly_noLogout() {
        when(resolver.resolve(session)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(session);

        assertSame(validConfig, result);
        verify(resolver).resolve(session);
        verify(session, never()).logout();
    }

    // ── Part 5A: cache invalidation ───────────────────────────────────────────

    @Test
    void invalidate_clearsCacheSoNextGetRefetches() {
        when(resolver.resolve(session)).thenReturn(validConfig);

        provider.get(() -> session);    // populates cache
        provider.invalidate();          // clears cache
        provider.get(() -> session);    // should re-fetch

        verify(resolver, times(2)).resolve(session);
    }

    // ── concurrency: resolver must be called at most once under concurrent first access ──

    @Test
    void concurrentFirstAccess_resolverCalledOnlyOnce() throws Exception {
        int threadCount = 20;
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        AtomicInteger resolveCount = new AtomicInteger();
        CachingDiscoveryConfigProvider freshProvider =
                new CachingDiscoveryConfigProvider(resolver, () -> session);

        when(resolver.resolve(session)).thenAnswer(inv -> {
            resolveCount.incrementAndGet();
            Thread.sleep(5); // widen race window so concurrent threads stack up
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
                "resolver.resolve() should be called exactly once; actual: " + resolveCount.get());
    }

    @Test
    void get_envVarUpdatedAfterCachePopulated_newValueReflected() {
        DiscoveryConfig cachedBase = new DiscoveryConfig(
                "acct", "domain", null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION", 10, "");
        DiscoveryConfig withEnvCreds = new DiscoveryConfig(
                "acct", "domain", "env-api-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION", 10, "");
        when(resolver.resolve(session)).thenReturn(cachedBase);
        when(resolver.applyEnvSysCredentials(cachedBase)).thenReturn(withEnvCreds);

        DiscoveryConfig result1 = provider.get(session);
        DiscoveryConfig result2 = provider.get(session);

        assertEquals("env-api-key", result1.apiKey());
        assertEquals("env-api-key", result2.apiKey());
        verify(resolver, times(1)).resolve(session);
        verify(resolver, times(2)).applyEnvSysCredentials(cachedBase);
    }
}
