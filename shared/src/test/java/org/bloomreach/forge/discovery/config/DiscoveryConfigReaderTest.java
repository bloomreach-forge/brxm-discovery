package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
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
        assertEquals("https://suggest.dxpapi.com", config.autosuggestBaseUri());
        assertEquals("PRODUCTION", config.environment());
        assertEquals(20, config.defaultPageSize());
        assertEquals("price asc", config.defaultSort());
    }

    @Test
    void read_repositoryExceptionOnProperty_propagates() throws RepositoryException {
        when(configNode.hasProperty(anyString())).thenThrow(new RepositoryException("JCR error"));

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
        DiscoveryConfigReader envReader = new DiscoveryConfigReader(
                name -> ConfigDefaults.ACCOUNT_ID_ENV.equals(name) ? "env-acct" : null);
        stubJcrProperty(ConfigDefaults.ACCOUNT_ID_JCR, "jcr-acct");
        System.setProperty(ConfigDefaults.ACCOUNT_ID_SYS, "sys-acct");
        try {
            String result = envReader.credential(
                    Optional.of(configNode),
                    ConfigDefaults.ACCOUNT_ID_ENV,
                    ConfigDefaults.ACCOUNT_ID_SYS,
                    ConfigDefaults.ACCOUNT_ID_JCR);
            assertEquals("env-acct", result);
        } finally {
            System.clearProperty(ConfigDefaults.ACCOUNT_ID_SYS);
        }
    }

    @Test
    void credential_sysPropWhenNoEnvVar() throws RepositoryException {
        stubJcrProperty(ConfigDefaults.DOMAIN_KEY_JCR, "jcr-domain");
        System.setProperty(ConfigDefaults.DOMAIN_KEY_SYS, "sys-domain");
        try {
            String result = reader.credential(
                    Optional.of(configNode),
                    ConfigDefaults.DOMAIN_KEY_ENV,
                    ConfigDefaults.DOMAIN_KEY_SYS,
                    ConfigDefaults.DOMAIN_KEY_JCR);
            assertEquals("sys-domain", result);
        } finally {
            System.clearProperty(ConfigDefaults.DOMAIN_KEY_SYS);
        }
    }

    @Test
    void credential_jcrFallbackWhenNoEnvOrSys() throws RepositoryException {
        // credential() is used for accountId/domainKey/environment (not apiKey/authKey)
        stubJcrProperty(ConfigDefaults.ACCOUNT_ID_JCR, "jcr-acct");
        String result = reader.credential(
                Optional.of(configNode),
                ConfigDefaults.ACCOUNT_ID_ENV,
                ConfigDefaults.ACCOUNT_ID_SYS,
                ConfigDefaults.ACCOUNT_ID_JCR);
        assertEquals("jcr-acct", result);
    }

    @Test
    void credential_nullWhenMissingEverywhere() throws RepositoryException {
        when(configNode.hasProperty(ConfigDefaults.ACCOUNT_ID_JCR)).thenReturn(false);
        String result = reader.credential(
                Optional.of(configNode),
                ConfigDefaults.ACCOUNT_ID_ENV,
                ConfigDefaults.ACCOUNT_ID_SYS,
                ConfigDefaults.ACCOUNT_ID_JCR);
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
            assertEquals(ConfigDefaults.AUTOSUGGEST_BASE_URI, config.autosuggestBaseUri());
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
        assertEquals(ConfigDefaults.AUTOSUGGEST_BASE_URI, config.autosuggestBaseUri());
    }

    @Test
    void readWithDefaults_stagingEnvironment_usesStagingBaseUriDefaults() {
        System.setProperty(ConfigDefaults.ENVIRONMENT_SYS, ConfigDefaults.STAGING_ENVIRONMENT);
        try {
            DiscoveryConfig config = reader.readWithDefaults();
            assertEquals(ConfigDefaults.STAGING_BASE_URI, config.baseUri());
            assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, config.pathwaysBaseUri());
            assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, config.autosuggestBaseUri());
            assertEquals(ConfigDefaults.STAGING_ENVIRONMENT, config.environment());
        } finally {
            System.clearProperty(ConfigDefaults.ENVIRONMENT_SYS);
        }
    }

    @Test
    void read_stagingEnvironmentWithoutExplicitUris_usesStagingDefaults() throws RepositoryException {
        stubJcrProperty(ConfigDefaults.ENVIRONMENT_JCR, ConfigDefaults.STAGING_ENVIRONMENT);
        lenient().when(configNode.hasProperty(ConfigDefaults.BASE_URI_JCR)).thenReturn(false);
        lenient().when(configNode.hasProperty(ConfigDefaults.PATHWAYS_BASE_URI_JCR)).thenReturn(false);
        lenient().when(configNode.hasProperty(ConfigDefaults.AUTOSUGGEST_BASE_URI_JCR)).thenReturn(false);

        DiscoveryConfig config = reader.read(configNode);

        assertEquals(ConfigDefaults.STAGING_BASE_URI, config.baseUri());
        assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, config.pathwaysBaseUri());
        assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, config.autosuggestBaseUri());
    }

    // --- credentialsFromEnvSys ---

    @Test
    void credential_blankJcrValue_returnsNull() throws RepositoryException {
        stubJcrProperty(ConfigDefaults.API_KEY_JCR, "");
        String result = reader.credential(
                Optional.of(configNode),
                ConfigDefaults.API_KEY_ENV,
                ConfigDefaults.API_KEY_SYS,
                ConfigDefaults.API_KEY_JCR);
        assertNull(result);
    }

    @Test
    void credentialsFromEnvSys_envLookupReturnsApiKey_populatesApiKey() {
        DiscoveryConfigReader envReader = new DiscoveryConfigReader(
                name -> "BRXDIS_API_KEY".equals(name) ? "env-api-key" : null);
        DiscoveryConfig result = envReader.credentialsFromEnvSys();
        assertEquals("env-api-key", result.apiKey());
        assertNull(result.accountId());
        assertNull(result.baseUri());
        assertNull(result.defaultSort());
        assertEquals(0, result.defaultPageSize());
    }

    @Test
    void credentialsFromEnvSys_nothingSet_allCredentialsNull() {
        DiscoveryConfig result = reader.credentialsFromEnvSys();
        assertNull(result.accountId());
        assertNull(result.domainKey());
        assertNull(result.apiKey());
        assertNull(result.authKey());
        assertNull(result.environment());
        assertNull(result.baseUri());
        assertNull(result.pathwaysBaseUri());
        assertNull(result.autosuggestBaseUri());
        assertEquals(0, result.defaultPageSize());
        assertNull(result.defaultSort());
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
        assertEquals("https://suggest.dxpapi.com", config.autosuggestBaseUri());
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
        stubJcrProperty("brxdis:apiKey",  "secret-key");
        stubJcrProperty("brxdis:authKey", "auth-key-123");
        stubJcrProperty("brxdis:environment", "PRODUCTION");
        stubJcrProperty("brxdis:baseUri", "https://core.dxpapi.com");
        stubJcrProperty("brxdis:pathwaysBaseUri", "https://pathways.dxpapi.com");
        stubJcrProperty("brxdis:autosuggestBaseUri", "https://suggest.dxpapi.com");

        Property pageSizeProp = mock(Property.class);
        lenient().when(configNode.hasProperty("brxdis:defaultPageSize")).thenReturn(true);
        lenient().when(configNode.getProperty("brxdis:defaultPageSize")).thenReturn(pageSizeProp);
        lenient().when(pageSizeProp.getLong()).thenReturn(20L);

        stubJcrProperty("brxdis:defaultSort", "price asc");
    }
}
