package org.bloomreach.forge.discovery.cms.picker;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryPickerModuleTest {

    private static final String SEARCH_NODE_PATH =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoverySearchAPI";
    private static final String PATHWAYS_NODE_PATH =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoveryPathwaysAPI";
    private static final String AUTOSUGGEST_NODE_PATH =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoveryAutosuggestAPI";
    private static final String PIXEL_NODE_PATH =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoveryPixelAPI";

    @Mock Session session;
    @Mock Node searchNode;
    @Mock Node pathwaysNode;
    @Mock Node autosuggestNode;
    @Mock Node pixelNode;

    @AfterEach
    void clearSysProp() {
        System.clearProperty("brxdis.pixelBaseUri");
    }

    @Test
    void initialize_syncsDiscoveryBaseUrisFromResolvedConfig() throws RepositoryException {
        DiscoveryConfig config = new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://staging-core.dxpapi.com",
                "https://staging-pathways.dxpapi.com",
                "https://staging-suggest.dxpapi.com",
                "STAGING", 12, "");
        when(session.nodeExists(SEARCH_NODE_PATH)).thenReturn(true);
        when(session.nodeExists(PATHWAYS_NODE_PATH)).thenReturn(true);
        when(session.nodeExists(AUTOSUGGEST_NODE_PATH)).thenReturn(true);
        when(session.getNode(SEARCH_NODE_PATH)).thenReturn(searchNode);
        when(session.getNode(PATHWAYS_NODE_PATH)).thenReturn(pathwaysNode);
        when(session.getNode(AUTOSUGGEST_NODE_PATH)).thenReturn(autosuggestNode);

        DiscoveryPickerModule module = new DiscoveryPickerModule(key -> null);
        module.applyCrispBaseUriOverrides(session, config);

        verify(searchNode).setProperty(eq("crisp:propvalues"), eq(new String[]{"https://staging-core.dxpapi.com"}));
        verify(pathwaysNode).setProperty(eq("crisp:propvalues"), eq(new String[]{"https://staging-pathways.dxpapi.com"}));
        verify(autosuggestNode).setProperty(eq("crisp:propvalues"), eq(new String[]{"https://staging-suggest.dxpapi.com"}));
        verify(session).save();
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
