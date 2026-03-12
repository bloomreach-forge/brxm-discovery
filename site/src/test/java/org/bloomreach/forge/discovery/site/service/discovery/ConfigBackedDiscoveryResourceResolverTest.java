package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigBackedDiscoveryResourceResolverTest {

    @Test
    void resolveFullUri_usesConfiguredSearchBaseUri() throws Exception {
        DiscoveryConfigProvider configProvider = mock(DiscoveryConfigProvider.class);
        when(configProvider.get(null)).thenReturn(new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://staging-core.dxpapi.com",
                "https://staging-pathways.dxpapi.com",
                "https://staging-suggest.dxpapi.com",
                "STAGING", 12, ""));

        ConfigBackedDiscoveryResourceResolver resolver = new ConfigBackedDiscoveryResourceResolver();
        resolver.setConfigProvider(configProvider);
        resolver.setApi("search");

        URI uri = resolver.resolveFullURI("/api/v1/core/?q=shirt");

        assertEquals("https://staging-core.dxpapi.com/api/v1/core/?q=shirt", uri.toString());
    }

    @Test
    void resolveFullUri_usesConfiguredAutosuggestBaseUri() throws Exception {
        DiscoveryConfigProvider configProvider = mock(DiscoveryConfigProvider.class);
        when(configProvider.get(null)).thenReturn(new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://core.dxpapi.com",
                "https://pathways.dxpapi.com",
                "https://suggest.dxpapi.com",
                "PRODUCTION", 12, ""));

        ConfigBackedDiscoveryResourceResolver resolver = new ConfigBackedDiscoveryResourceResolver();
        resolver.setConfigProvider(configProvider);
        resolver.setApi("autosuggest");

        URI uri = resolver.resolveFullURI("/api/v2/suggest/?q=sh");

        assertEquals("https://suggest.dxpapi.com/api/v2/suggest/?q=sh", uri.toString());
    }
}
