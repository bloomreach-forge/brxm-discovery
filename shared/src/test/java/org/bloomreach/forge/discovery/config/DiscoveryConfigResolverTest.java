package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryConfigResolverTest {

    @Mock DiscoveryConfigReader configReader;
    @Mock Session session;
    @Mock Node configNode;

    private DiscoveryConfig validConfig;
    private DiscoveryConfigResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DiscoveryConfigResolver(configReader);
        validConfig = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
    }

    @Test
    void resolve_returnsConfig() throws RepositoryException {
        when(session.getNode(ConfigDefaults.CONFIG_NODE_PATH)).thenReturn(configNode);
        when(configReader.read(configNode)).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session);

        assertSame(validConfig, result);
    }

    @Test
    void resolve_nodeNotFound_fallsBackToDefaults() throws RepositoryException {
        when(session.getNode(ConfigDefaults.CONFIG_NODE_PATH)).thenThrow(new PathNotFoundException("not found"));
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session);

        assertSame(validConfig, result);
    }

    @Test
    void resolve_repositoryException_wrapsInConfigurationException() throws RepositoryException {
        when(session.getNode(ConfigDefaults.CONFIG_NODE_PATH)).thenThrow(new RepositoryException("JCR error"));

        assertThrows(ConfigurationException.class, () -> resolver.resolve(session));
    }

    @Test
    void applyEnvSysCredentials_envHasApiKey_overridesBaseBlank() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION", 10, "");
        DiscoveryCredentials envCreds = new DiscoveryCredentials(
                null, null, "env-api-key", null, null);
        when(configReader.credentialsFromEnvSysOnly()).thenReturn(envCreds);

        DiscoveryConfig result = resolver.applyEnvSysCredentials(base);

        assertEquals("env-api-key", result.apiKey());
        assertEquals("acct", result.accountId());
        assertEquals("domain", result.domainKey());
    }

    @Test
    void applyEnvSysCredentials_structuralAlwaysFromBase() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://custom.api.com", "https://custom-pathways.com", "STAGING", 20, "price asc");
        when(configReader.credentialsFromEnvSysOnly()).thenReturn(new DiscoveryCredentials(
                null, null, null, null, null));

        DiscoveryConfig result = resolver.applyEnvSysCredentials(base);

        assertEquals("https://custom.api.com", result.baseUri());
        assertEquals("https://custom-pathways.com", result.pathwaysBaseUri());
        assertEquals(20, result.defaultPageSize());
        assertEquals("price asc", result.defaultSort());
    }

    @Test
    void resolveDefaults_callsReadWithDefaults() {
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolveDefaults();

        assertSame(validConfig, result);
        verifyNoInteractions(session);
    }
}
