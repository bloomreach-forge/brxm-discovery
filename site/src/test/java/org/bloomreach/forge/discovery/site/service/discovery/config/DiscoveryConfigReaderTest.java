package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DiscoveryConfigReaderTest {

    private DiscoveryConfigReader reader;

    @Mock Node configNode;

    @BeforeEach
    void setUp() {
        // Suppress env var lookup so host machine env vars don't bleed into tests
        reader = new DiscoveryConfigReader(ignored -> null);
    }

    @Test
    void read_allProperties_populatesConfigCorrectly() throws RepositoryException {
        stubAllProperties();

        DiscoveryConfig config = reader.read(configNode);

        assertEquals("acct123", config.accountId());
        assertEquals("myDomain", config.domainKey());
        assertEquals("secret-key", config.apiKey());
        assertEquals("auth-key-123", config.authKey());
        assertEquals("https://core.dxpapi.com", config.baseUri());
        assertEquals("https://pathways.dxpapi.com", config.pathwaysBaseUri());
        assertEquals("PRODUCTION", config.environment());
        assertEquals(20, config.defaultPageSize());
        assertEquals("price asc", config.defaultSort());
    }

    @Test
    void read_repositoryExceptionOnProperty_propagates() throws RepositoryException {
        when(configNode.hasProperty("brxdis:accountId")).thenThrow(new RepositoryException("JCR error"));

        assertThrows(RepositoryException.class, () -> reader.read(configNode));
    }

    @Test
    void read_systemPropertySet_takesOverJcrValue() throws RepositoryException {
        stubAllProperties();
        System.setProperty("brxdis.accountId", "sys-override");
        try {
            assertEquals("sys-override", reader.read(configNode).accountId());
        } finally {
            System.clearProperty("brxdis.accountId");
        }
    }

    @Test
    void read_systemPropertySet_domainKeyOverridden() throws RepositoryException {
        stubAllProperties();
        System.setProperty("brxdis.domainKey", "sys-domain");
        try {
            assertEquals("sys-domain", reader.read(configNode).domainKey());
        } finally {
            System.clearProperty("brxdis.domainKey");
        }
    }

    @Test
    void read_systemPropertySet_apiKeyOverridden() throws RepositoryException {
        stubAllProperties();
        System.setProperty("brxdis.apiKey", "sys-key");
        try {
            assertEquals("sys-key", reader.read(configNode).apiKey());
        } finally {
            System.clearProperty("brxdis.apiKey");
        }
    }

    @Test
    void read_noExternalOverride_jcrValueUsedAsFallback() throws RepositoryException {
        stubAllProperties();
        assertEquals("acct123", reader.read(configNode).accountId());
    }

    // --- credential resolution ---

    @Test
    void credential_envVarTakesPrecedence() throws RepositoryException {
        // Env vars can't be set in tests easily, but sys prop can be tested
        // This test validates the precedence: sys prop overrides JCR
        stubJcrProperty("brxdis:accountId", "jcr-acct");
        System.setProperty("brxdis.accountId", "sys-acct");
        try {
            String result = reader.credential(
                    Optional.of(configNode), "brxdis:accountId", "brxdis.accountId");
            assertEquals("sys-acct", result);
        } finally {
            System.clearProperty("brxdis.accountId");
        }
    }

    @Test
    void credential_sysPropWhenNoEnvVar() throws RepositoryException {
        stubJcrProperty("brxdis:domainKey", "jcr-domain");
        System.setProperty("brxdis.domainKey", "sys-domain");
        try {
            String result = reader.credential(
                    Optional.of(configNode), "brxdis:domainKey", "brxdis.domainKey");
            assertEquals("sys-domain", result);
        } finally {
            System.clearProperty("brxdis.domainKey");
        }
    }

    @Test
    void credential_jcrFallbackWhenNoEnvOrSys() throws RepositoryException {
        stubJcrProperty("brxdis:apiKey", "jcr-key");
        String result = reader.credential(
                Optional.of(configNode), "brxdis:apiKey", "brxdis.apiKey");
        assertEquals("jcr-key", result);
    }

    @Test
    void credential_nullWhenMissingEverywhere() throws RepositoryException {
        when(configNode.hasProperty("brxdis:authKey")).thenReturn(false);
        String result = reader.credential(
                Optional.of(configNode), "brxdis:authKey", "brxdis.authKey");
        assertNull(result);
    }

    // --- structural resolution ---

    @Test
    void structural_jcrTakesPrecedence() throws RepositoryException {
        stubJcrProperty("brxdis:baseUri", "https://custom.api.com");
        String result = DiscoveryConfigReader.structural(
                Optional.of(configNode), "brxdis:baseUri", ConfigDefaults.BASE_URI);
        assertEquals("https://custom.api.com", result);
    }

    @Test
    void structural_codedDefaultWhenNoJcr() throws RepositoryException {
        when(configNode.hasProperty("brxdis:baseUri")).thenReturn(false);
        String result = DiscoveryConfigReader.structural(
                Optional.of(configNode), "brxdis:baseUri", ConfigDefaults.BASE_URI);
        assertEquals("https://core.dxpapi.com", result);
    }

    // --- readWithDefaults ---

    @Test
    void readWithDefaults_noJcrNode_credentialsFromSys_structuralFromDefaults() {
        System.setProperty("brxdis.accountId", "env-acct");
        System.setProperty("brxdis.domainKey", "env-domain");
        System.setProperty("brxdis.apiKey", "env-key");
        try {
            DiscoveryConfig config = reader.readWithDefaults();
            assertEquals("env-acct", config.accountId());
            assertEquals("env-domain", config.domainKey());
            assertEquals("env-key", config.apiKey());
            assertNull(config.authKey());
            assertEquals(ConfigDefaults.BASE_URI, config.baseUri());
            assertEquals(ConfigDefaults.PATHWAYS_BASE_URI, config.pathwaysBaseUri());
            assertEquals(ConfigDefaults.ENVIRONMENT, config.environment());
            assertEquals(ConfigDefaults.DEFAULT_PAGE_SIZE, config.defaultPageSize());
            assertEquals(ConfigDefaults.DEFAULT_SORT, config.defaultSort());
        } finally {
            System.clearProperty("brxdis.accountId");
            System.clearProperty("brxdis.domainKey");
            System.clearProperty("brxdis.apiKey");
        }
    }

    @Test
    void readWithDefaults_noCredentials_returnsNullCredentials() {
        DiscoveryConfig config = reader.readWithDefaults();
        assertNull(config.accountId());
        assertNull(config.domainKey());
        assertNull(config.apiKey());
        assertEquals(ConfigDefaults.BASE_URI, config.baseUri());
    }

    @Test
    void read_jcrPresent_fullConfig() throws RepositoryException {
        stubAllProperties();
        DiscoveryConfig config = reader.read(configNode);
        assertEquals("acct123", config.accountId());
        assertEquals("myDomain", config.domainKey());
        assertEquals("secret-key", config.apiKey());
        assertEquals("auth-key-123", config.authKey());
        assertEquals("https://core.dxpapi.com", config.baseUri());
        assertEquals("https://pathways.dxpapi.com", config.pathwaysBaseUri());
        assertEquals("PRODUCTION", config.environment());
        assertEquals(20, config.defaultPageSize());
        assertEquals("price asc", config.defaultSort());
    }

    // --- helpers ---

    private void stubJcrProperty(String name, String value) throws RepositoryException {
        Property prop = mock(Property.class);
        lenient().when(configNode.hasProperty(name)).thenReturn(true);
        lenient().when(configNode.getProperty(name)).thenReturn(prop);
        lenient().when(prop.getString()).thenReturn(value);
    }

    private void stubAllProperties() throws RepositoryException {
        stubJcrProperty("brxdis:accountId", "acct123");
        stubJcrProperty("brxdis:domainKey", "myDomain");
        stubJcrProperty("brxdis:apiKey", "secret-key");
        stubJcrProperty("brxdis:authKey", "auth-key-123");
        stubJcrProperty("brxdis:environment", "PRODUCTION");
        stubJcrProperty("brxdis:baseUri", "https://core.dxpapi.com");
        stubJcrProperty("brxdis:pathwaysBaseUri", "https://pathways.dxpapi.com");

        Property pageSizeProp = mock(Property.class);
        lenient().when(configNode.hasProperty("brxdis:defaultPageSize")).thenReturn(true);
        lenient().when(configNode.getProperty("brxdis:defaultPageSize")).thenReturn(pageSizeProp);
        lenient().when(pageSizeProp.getLong()).thenReturn(20L);

        stubJcrProperty("brxdis:defaultSort", "price asc");
    }
}
