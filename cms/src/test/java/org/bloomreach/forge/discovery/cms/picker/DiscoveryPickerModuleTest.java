package org.bloomreach.forge.discovery.cms.picker;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onehippo.cms7.services.HippoServiceRegistry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPickerModuleTest {

    private static final String PIXEL_NODE_PATH =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoveryPixelAPI";

    @Mock Session session;
    @Mock Node pixelNode;
    @Mock DiscoveryConfigProvider configProvider;

    @AfterEach
    void clearState() {
        System.clearProperty("brxdis.pixelBaseUri");
        HippoServiceRegistry.unregister(configProvider, DiscoveryConfigProvider.class);
    }

    @Test
    void registerConfigProvider_exposesProviderViaHippoServiceRegistry() {
        DiscoveryPickerModule module = new DiscoveryPickerModule(key -> null);
        module.registerConfigProvider(configProvider);

        assertNotNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));
    }

    @Test
    void unregisterConfigProvider_removesProviderFromHippoServiceRegistry() {
        DiscoveryPickerModule module = new DiscoveryPickerModule(key -> null);
        module.registerConfigProvider(configProvider);

        module.unregisterConfigProvider(configProvider);

        assertNull(HippoServiceRegistry.getService(DiscoveryConfigProvider.class));
    }

    @Test
    void applyPixelBaseUriOverride_withEnvVar_updatesPixelCrispNode() throws RepositoryException {
        when(session.nodeExists(PIXEL_NODE_PATH)).thenReturn(true);
        when(session.getNode(PIXEL_NODE_PATH)).thenReturn(pixelNode);

        DiscoveryPickerModule module = new DiscoveryPickerModule(
                key -> "BRXDIS_PIXEL_BASEURI".equals(key) ? "https://p-eu.brsrvr.com" : null);
        module.applyPixelBaseUriOverride(session);

        verify(pixelNode).setProperty(eq("crisp:propvalues"), eq(new String[]{"https://p-eu.brsrvr.com"}));
        verify(session, never()).save();
    }

    @Test
    void applyPixelBaseUriOverride_withSysProp_updatesPixelCrispNode() throws RepositoryException {
        System.setProperty("brxdis.pixelBaseUri", "https://p-eu.brsrvr.com");
        when(session.nodeExists(PIXEL_NODE_PATH)).thenReturn(true);
        when(session.getNode(PIXEL_NODE_PATH)).thenReturn(pixelNode);

        DiscoveryPickerModule module = new DiscoveryPickerModule(key -> null);
        module.applyPixelBaseUriOverride(session);

        verify(pixelNode).setProperty(eq("crisp:propvalues"), eq(new String[]{"https://p-eu.brsrvr.com"}));
        verify(session, never()).save();
    }

    @Test
    void applyPixelBaseUriOverride_noOverride_doesNotUpdateNode() throws RepositoryException {
        DiscoveryPickerModule module = new DiscoveryPickerModule(key -> null);
        module.applyPixelBaseUriOverride(session);

        verify(pixelNode, never()).setProperty(anyString(), any(String[].class));
        verify(session, never()).save();
    }

    @Test
    void applyPixelBaseUriOverride_missingCrispNode_doesNotThrow() throws RepositoryException {
        when(session.nodeExists(PIXEL_NODE_PATH)).thenReturn(false);

        DiscoveryPickerModule module = new DiscoveryPickerModule(
                key -> "BRXDIS_PIXEL_BASEURI".equals(key) ? "https://p-eu.brsrvr.com" : null);

        assertDoesNotThrow(() -> module.applyPixelBaseUriOverride(session));
        verify(pixelNode, never()).setProperty(anyString(), any(String[].class));
        verify(session, never()).save();
    }
}
