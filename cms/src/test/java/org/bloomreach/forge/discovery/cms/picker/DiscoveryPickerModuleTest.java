package org.bloomreach.forge.discovery.cms.picker;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class DiscoveryPickerModuleTest {

    @Mock DiscoveryConfigProvider configProvider;

    @AfterEach
    void clearState() {
        HippoServiceRegistry.unregister(configProvider, DiscoveryConfigProvider.class);
    }

    @Test
    void registerConfigProvider_exposesProviderViaHippoServiceRegistry() {
        DiscoveryPickerModule module = new DiscoveryPickerModule();
        module.registerConfigProvider(configProvider);

        assertNotNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));
    }

    @Test
    void unregisterConfigProvider_removesProviderFromHippoServiceRegistry() {
        DiscoveryPickerModule module = new DiscoveryPickerModule();
        module.registerConfigProvider(configProvider);

        module.unregisterConfigProvider(configProvider);

        assertNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));
    }

    @Test
    void shutdown_withoutInitialize_doesNotThrow() {
        DiscoveryPickerModule module = new DiscoveryPickerModule();

        assertDoesNotThrow(module::shutdown);
    }
}
