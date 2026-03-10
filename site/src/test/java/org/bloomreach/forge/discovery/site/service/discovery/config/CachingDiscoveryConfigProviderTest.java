package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
    }

    @Test
    void get_nullPath_callsResolverWithNullNull() {
        when(resolver.resolve(null, null)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(null, () -> session);

        assertSame(validConfig, result);
        verify(resolver).resolve(null, null);
        verifyNoInteractions(session);
    }

    @Test
    void get_blankPath_normalizesToSameKeyAsNull() {
        when(resolver.resolve(null, null)).thenReturn(validConfig);

        provider.get(null, () -> session);
        DiscoveryConfig result = provider.get("  ", () -> session);

        assertSame(validConfig, result);
        // Second call with blank path hits cache — resolver called only once
        verify(resolver, times(1)).resolve(any(), any());
        verifyNoInteractions(session);
    }

    @Test
    void get_validPath_callsResolverWithAdminSession() {
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(session, path)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get(path, () -> session);

        assertSame(validConfig, result);
        verify(resolver).resolve(session, path);
        verify(session).logout();
    }

    @Test
    void get_populatesCache_secondCallSkipsResolver() {
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(session, path)).thenReturn(validConfig);

        provider.get(path, () -> session);
        provider.get(path, () -> session);

        verify(resolver, times(1)).resolve(any(), any());
    }

    @Test
    void get_resolverThrows_sessionLoggedOut() {
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(session, path)).thenThrow(new ConfigurationException("no creds"));

        assertThrows(ConfigurationException.class, () -> provider.get(path, () -> session));
        verify(session).logout();
    }

    @Test
    void get_nullAndValidPathAreDifferentCacheKeys() {
        DiscoveryConfig defaultConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                12, "relevance");
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(null, null)).thenReturn(defaultConfig);
        when(resolver.resolve(session, path)).thenReturn(validConfig);

        DiscoveryConfig result1 = provider.get(null, () -> session);
        DiscoveryConfig result2 = provider.get(path, () -> session);

        assertNotSame(result1, result2);
        assertSame(defaultConfig, result1);
        assertSame(validConfig, result2);
    }

    @Test
    void invalidate_removesSpecificKey_nextGetResolves() {
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(session, path)).thenReturn(validConfig);

        provider.get(path, () -> session);
        provider.invalidate(path);
        provider.get(path, () -> session);

        verify(resolver, times(2)).resolve(session, path);
    }

    @Test
    void invalidate_normalizesBlanksConsistently() {
        when(resolver.resolve(null, null)).thenReturn(validConfig);

        provider.get(null, () -> session);
        provider.invalidate("  "); // blank normalizes same as null/empty
        provider.get("", () -> session); // should re-resolve

        verify(resolver, times(2)).resolve(any(), any());
    }

    @Test
    void invalidateAll_clearsCache() {
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(null, null)).thenReturn(validConfig);
        when(resolver.resolve(session, path)).thenReturn(validConfig);

        provider.get(null, () -> session);
        provider.get(path, () -> session);
        provider.invalidateAll();
        provider.get(null, () -> session);
        provider.get(path, () -> session);

        verify(resolver, times(4)).resolve(any(), any());
    }

    @Test
    void get_sessionSupplierThrows_fallsBackToDefaults() {
        when(resolver.resolve(null, null)).thenReturn(validConfig);

        DiscoveryConfig result = provider.get("/some/path", () -> { throw new RuntimeException("JCR unavailable"); });

        assertSame(validConfig, result);
        verify(resolver).resolve(null, null);
    }

    @Test
    void get_jcrSessionFailure_cachesFallback_avoidsPerRequestSysPropReads() {
        // Partial config from env/sys props when JCR session is unavailable
        DiscoveryConfig partial = new DiscoveryConfig(
                null, null, "global-key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        String path = "/hippo:configuration/discoveryConfig";
        when(resolver.resolve(null, null)).thenReturn(partial);

        CachingDiscoveryConfigProvider.SessionSupplier failingSupplier =
                () -> { throw new RuntimeException("JCR not yet available"); };

        // First call: session unavailable → falls back to env/sys props
        DiscoveryConfig result1 = provider.get(path, failingSupplier);
        assertSame(partial, result1);

        // Second call: cache hit — resolver NOT called again (no per-request sys-prop reads)
        DiscoveryConfig result2 = provider.get(path, failingSupplier);
        assertSame(partial, result2);

        // Stale partial config is safe: patchFromChannelInfo in HstDiscoveryService
        // always overrides accountId/domainKey from Channel Manager and re-evaluates
        // per-channel env vars for apiKey/authKey on every request.
        verify(resolver, times(1)).resolve(null, null);
    }
}
