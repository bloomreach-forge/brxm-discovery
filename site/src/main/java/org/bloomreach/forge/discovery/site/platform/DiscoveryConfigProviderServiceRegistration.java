package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.onehippo.cms7.services.HippoServiceRegistry;

/**
 * Registers the site DiscoveryConfigProvider in HippoServiceRegistry so sibling addon-module
 * contexts such as CRISP can resolve it without direct Spring bean references.
 */
public class DiscoveryConfigProviderServiceRegistration {

    private final DiscoveryConfigProvider configProvider;

    public DiscoveryConfigProviderServiceRegistration(DiscoveryConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public void register() {
        HippoServiceRegistry.registerService(configProvider, DiscoveryConfigProvider.class);
    }

    public void unregister() {
        HippoServiceRegistry.unregisterService(configProvider, DiscoveryConfigProvider.class);
    }
}
