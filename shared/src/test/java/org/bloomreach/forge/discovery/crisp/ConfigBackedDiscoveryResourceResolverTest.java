package org.bloomreach.forge.discovery.crisp;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigBackedDiscoveryResourceResolverTest {

    @Test
    void resolveFullUri_usesInjectedProvider() throws Exception {
        DiscoveryConfigProvider configProvider = mock(DiscoveryConfigProvider.class);
        when(configProvider.settings()).thenReturn(new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://staging-core.dxpapi.com",
                "https://staging-pathways.dxpapi.com",
                "https://staging-suggest.dxpapi.com",
                "STAGING", 12, "").settings());

        ConfigBackedDiscoveryResourceResolver resolver = new ConfigBackedDiscoveryResourceResolver();
        resolver.setConfigProvider(configProvider);
        resolver.setApi("search");

        URI uri = resolver.resolveFullURI("/api/v1/core/?q=shirt");

        assertEquals("https://staging-core.dxpapi.com/api/v1/core/?q=shirt", uri.toString());
    }

    @Test
    void resolveFullUri_fallsBackToLookupProvider() throws Exception {
        DiscoveryConfigProvider configProvider = mock(DiscoveryConfigProvider.class);
        when(configProvider.settings()).thenReturn(new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://core.dxpapi.com",
                "https://pathways.dxpapi.com",
                "https://suggest.dxpapi.com",
                "PRODUCTION", 12, "").settings());

        TestConfigBackedDiscoveryResourceResolver resolver = new TestConfigBackedDiscoveryResourceResolver(configProvider);
        resolver.setApi("autosuggest");

        URI uri = resolver.resolveFullURI("/api/v2/suggest/?q=sh");

        assertEquals("https://suggest.dxpapi.com/api/v2/suggest/?q=sh", uri.toString());
        assertEquals(1, resolver.lookupCount());
    }

    @Test
    void resolveFullUri_lookupIsCachedAfterFirstResolution() throws Exception {
        DiscoveryConfigProvider configProvider = mock(DiscoveryConfigProvider.class);
        when(configProvider.settings()).thenReturn(new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://core.dxpapi.com",
                "https://pathways.dxpapi.com",
                "https://suggest.dxpapi.com",
                "PRODUCTION", 12, "").settings());

        TestConfigBackedDiscoveryResourceResolver resolver = new TestConfigBackedDiscoveryResourceResolver(configProvider);
        resolver.setApi("search");

        resolver.resolveFullURI("/api/v1/core/?q=one");
        URI uri = resolver.resolveFullURI("/api/v1/core/?q=two");

        assertEquals(1, resolver.lookupCount());
        assertEquals("https://core.dxpapi.com/api/v1/core/?q=two", uri.toString());
    }

    private static final class TestConfigBackedDiscoveryResourceResolver extends ConfigBackedDiscoveryResourceResolver {

        private final DiscoveryConfigProvider discoveryConfigProvider;
        private final AtomicInteger lookupCount = new AtomicInteger();

        private TestConfigBackedDiscoveryResourceResolver(DiscoveryConfigProvider discoveryConfigProvider) {
            this.discoveryConfigProvider = discoveryConfigProvider;
        }

        @Override
        protected DiscoveryConfigProvider lookupConfigProvider() {
            lookupCount.incrementAndGet();
            return discoveryConfigProvider;
        }

        private int lookupCount() {
            return lookupCount.get();
        }
    }
}
