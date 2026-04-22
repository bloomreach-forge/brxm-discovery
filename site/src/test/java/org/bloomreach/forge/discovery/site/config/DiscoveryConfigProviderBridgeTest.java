package org.bloomreach.forge.discovery.site.config;

import org.bloomreach.forge.discovery.config.CachingDiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryConfigProviderBridgeTest {

    @Mock DiscoveryConfigProvider registeredProvider;
    @Mock CachingDiscoveryConfigProvider fallback;
    @Mock Session session;

    private DiscoveryConfigProviderBridge bridgeWithRegistry() {
        return new DiscoveryConfigProviderBridge(() -> registeredProvider, () -> fallback);
    }

    private DiscoveryConfigProviderBridge bridgeWithoutRegistry() {
        return new DiscoveryConfigProviderBridge(() -> null, () -> fallback);
    }

    @Test
    void start_registryHasProvider_noFallbackStarted() {
        bridgeWithRegistry().start();
        verify(fallback, never()).start();
    }

    @Test
    void start_registryEmpty_fallbackStarted() {
        bridgeWithoutRegistry().start();
        verify(fallback).start();
    }

    @Test
    void get_delegatesToRegisteredProvider() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.get();
        verify(registeredProvider).get();
        verifyNoInteractions(fallback);
    }

    @Test
    void get_registryEmpty_delegatesToFallback() {
        var bridge = bridgeWithoutRegistry();
        bridge.start();
        bridge.get();
        verify(fallback).get();
    }

    @Test
    void getWithSession_delegatesToRegisteredProvider() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.get(session);
        verify(registeredProvider).get(session);
    }

    @Test
    void settings_delegatesToRegisteredProvider() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.settings();
        verify(registeredProvider).settings();
    }

    @Test
    void invalidate_delegatesToRegisteredProvider() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.invalidate();
        verify(registeredProvider).invalidate();
    }

    @Test
    void invalidateAll_delegatesToRegisteredProvider() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.invalidateAll();
        verify(registeredProvider).invalidateAll();
    }

    @Test
    void close_withFallbackActive_closesFallback() {
        var bridge = bridgeWithoutRegistry();
        bridge.start();
        bridge.close();
        verify(fallback).close();
    }

    @Test
    void close_withRegistryProvider_doesNotCloseFallback() {
        var bridge = bridgeWithRegistry();
        bridge.start();
        bridge.close();
        verifyNoInteractions(fallback);
    }
}
