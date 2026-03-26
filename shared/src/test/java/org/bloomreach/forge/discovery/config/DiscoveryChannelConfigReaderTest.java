package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryChannelConfigReaderTest {

    @Mock Session session;
    @Mock Node rootNode;
    @Mock Node hstRootNode;
    @Mock NodeIterator hstRootIterator;
    @Mock Node channelInfoNode;
    @Mock Property accountProp;
    @Mock Property domainProp;

    // ── resolveOverrides (HST path) ─────────────────────────────────────────

    @Test
    void resolveOverrides_allBlank_returnsNull() {
        assertNull(DiscoveryChannelConfigReader.resolveOverrides("", "", "", ""));
    }

    @Test
    void resolveOverrides_nullInputs_returnsNull() {
        assertNull(DiscoveryChannelConfigReader.resolveOverrides(null, null, null, null));
    }

    @Test
    void resolveOverrides_accountIdSet_returnedInCredentials() {
        DiscoveryCredentials creds = DiscoveryChannelConfigReader.resolveOverrides("myAcct", "", "", "");
        assertNotNull(creds);
        assertEquals("myAcct", creds.accountId());
        assertNull(creds.domainKey());
        assertNull(creds.apiKey());
        assertNull(creds.authKey());
    }

    @Test
    void resolveOverrides_apiKeyEnvVarSet_envVarResolved() {
        DiscoveryCredentials creds = DiscoveryChannelConfigReader.resolveOverrides(
                "", "", "MY_API_KEY_VAR", "",
                name -> "MY_API_KEY_VAR".equals(name) ? "resolved-key" : null);
        assertNotNull(creds);
        assertEquals("resolved-key", creds.apiKey());
    }

    @Test
    void resolveOverrides_apiKeyEnvVarSet_envVarMissing_returnsCredentialsWithNullApiKey() {
        DiscoveryCredentials creds = DiscoveryChannelConfigReader.resolveOverrides(
                "", "", "MY_API_KEY_VAR", "",
                name -> null);
        // param name is non-blank → channel opted-in; env var not found → apiKey is null
        assertNotNull(creds);
        assertNull(creds.apiKey());
    }

    @Test
    void resolveOverrides_authKeyEnvVarSet_envVarResolved() {
        DiscoveryCredentials creds = DiscoveryChannelConfigReader.resolveOverrides(
                "", "", "", "MY_AUTH_KEY_VAR",
                name -> "MY_AUTH_KEY_VAR".equals(name) ? "resolved-auth" : null);
        assertNotNull(creds);
        assertEquals("resolved-auth", creds.authKey());
    }

    // ── resolveFromJcr (CMS path) ───────────────────────────────────────────

    @Test
    void resolveFromJcr_channelIdBlank_returnsNull() throws RepositoryException {
        assertNull(DiscoveryChannelConfigReader.resolveFromJcr("", session));
        assertNull(DiscoveryChannelConfigReader.resolveFromJcr(null, session));
        verifyNoInteractions(session);
    }

    @Test
    void resolveFromJcr_nodeNotFound_returnsNull() throws RepositoryException {
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNodes("hst:*")).thenReturn(hstRootIterator);
        when(hstRootIterator.hasNext()).thenReturn(false);

        assertNull(DiscoveryChannelConfigReader.resolveFromJcr("myChannel", session));
    }

    @Test
    void resolveFromJcr_propsPresent_returnsCredentials() throws RepositoryException {
        String path = "/hst:myproject/hst:configurations/myChannel/hst:workspace/hst:channel/hst:channelinfo";
        when(session.getRootNode()).thenReturn(rootNode);
        when(rootNode.getNodes("hst:*")).thenReturn(hstRootIterator);
        when(hstRootIterator.hasNext()).thenReturn(true, false);
        when(hstRootIterator.nextNode()).thenReturn(hstRootNode);
        when(hstRootNode.getPath()).thenReturn("/hst:myproject");
        when(session.nodeExists(path)).thenReturn(true);
        when(session.getNode(path)).thenReturn(channelInfoNode);

        stubProp(DiscoveryChannelConfigReader.PARAM_ACCOUNT_ID, "chan-acct", accountProp);
        stubProp(DiscoveryChannelConfigReader.PARAM_DOMAIN_KEY, "chan-domain", domainProp);
        stubAbsent(DiscoveryChannelConfigReader.PARAM_API_KEY_ENV_VAR);
        stubAbsent(DiscoveryChannelConfigReader.PARAM_AUTH_KEY_ENV_VAR);

        DiscoveryCredentials creds = DiscoveryChannelConfigReader.resolveFromJcr(
                "myChannel", session,
                name -> null);  // no env vars in this test

        assertNotNull(creds);
        assertEquals("chan-acct", creds.accountId());
        assertEquals("chan-domain", creds.domainKey());
        assertNull(creds.apiKey());
        assertNull(creds.authKey());
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void stubProp(String name, String value, Property mock) throws RepositoryException {
        when(channelInfoNode.hasProperty(name)).thenReturn(true);
        when(channelInfoNode.getProperty(name)).thenReturn(mock);
        when(mock.getString()).thenReturn(value);
    }

    private void stubAbsent(String name) throws RepositoryException {
        when(channelInfoNode.hasProperty(name)).thenReturn(false);
    }
}
