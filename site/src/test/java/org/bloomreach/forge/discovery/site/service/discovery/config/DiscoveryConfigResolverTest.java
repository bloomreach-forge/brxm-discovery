package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
    void resolve_validPath_returnsConfig() throws RepositoryException {
        when(session.getNode("/content/discovery-config")).thenReturn(configNode);
        when(configReader.read(configNode)).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session, "/content/discovery-config");

        assertSame(validConfig, result);
    }

    @Test
    void resolve_nullConfigPath_usesDefaults() {
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session, null);

        assertSame(validConfig, result);
        verifyNoInteractions(session);
    }

    @Test
    void resolve_blankConfigPath_usesDefaults() {
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session, "   ");

        assertSame(validConfig, result);
        verifyNoInteractions(session);
    }

    @Test
    void resolve_nodeNotFound_fallsBackToDefaults() throws RepositoryException {
        when(session.getNode("/missing/path")).thenThrow(new PathNotFoundException("not found"));
        when(configReader.readWithDefaults()).thenReturn(validConfig);

        DiscoveryConfig result = resolver.resolve(session, "/missing/path");

        assertSame(validConfig, result);
    }

    @Test
    void resolve_repositoryException_wrapsInConfigurationException() throws RepositoryException {
        when(session.getNode("/bad/path")).thenThrow(new RepositoryException("JCR error"));

        assertThrows(ConfigurationException.class,
                () -> resolver.resolve(session, "/bad/path"));
    }

    @Test
    void resolve_noCredentialsAnywhere_throwsConfigurationException() {
        DiscoveryConfig noCredentials = new DiscoveryConfig(
                null, null, null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                12, "");
        when(configReader.readWithDefaults()).thenReturn(noCredentials);

        assertThrows(ConfigurationException.class,
                () -> resolver.resolve(session, null));
    }

    @Test
    void resolve_missingAccountId_throwsConfigurationException() throws RepositoryException {
        DiscoveryConfig noCreds = new DiscoveryConfig(
                null, "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
        when(session.getNode("/path")).thenReturn(configNode);
        when(configReader.read(configNode)).thenReturn(noCreds);

        assertThrows(ConfigurationException.class,
                () -> resolver.resolve(session, "/path"));
    }

    @Test
    void resolve_missingDomainKey_throwsConfigurationException() throws RepositoryException {
        DiscoveryConfig noCreds = new DiscoveryConfig(
                "acct", null, "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
        when(session.getNode("/path")).thenReturn(configNode);
        when(configReader.read(configNode)).thenReturn(noCreds);

        assertThrows(ConfigurationException.class,
                () -> resolver.resolve(session, "/path"));
    }

    @Test
    void resolve_missingApiKey_throwsConfigurationException() throws RepositoryException {
        DiscoveryConfig noCreds = new DiscoveryConfig(
                "acct", "domain", null, null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "PRODUCTION",
                10, "");
        when(session.getNode("/path")).thenReturn(configNode);
        when(configReader.read(configNode)).thenReturn(noCreds);

        assertThrows(ConfigurationException.class,
                () -> resolver.resolve(session, "/path"));
    }
}
