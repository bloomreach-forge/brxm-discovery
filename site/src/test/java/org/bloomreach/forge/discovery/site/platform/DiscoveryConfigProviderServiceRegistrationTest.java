package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class DiscoveryConfigProviderServiceRegistrationTest {

    private final DiscoveryConfigProvider provider = mock(DiscoveryConfigProvider.class);

    @AfterEach
    void tearDown() {
        HippoServiceRegistry.unregisterService(provider, DiscoveryConfigProvider.class);
    }

    @Test
    void registerAndUnregister_manageHippoServiceRegistryBinding() {
        DiscoveryConfigProviderServiceRegistration registration =
                new DiscoveryConfigProviderServiceRegistration(provider);

        registration.register();
        assertNotNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));

        registration.unregister();
        assertNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));
    }
}
