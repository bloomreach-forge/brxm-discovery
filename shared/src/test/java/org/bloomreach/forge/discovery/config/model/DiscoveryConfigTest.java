package org.bloomreach.forge.discovery.config.model;

import org.bloomreach.forge.discovery.config.ConfigDefaults;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryConfigTest {

    @Test
    void credentials_returnsCredentialView() {
        DiscoveryConfig config = new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "price asc");

        DiscoveryCredentials credentials = config.credentials();

        assertEquals("acct", credentials.accountId());
        assertEquals("domain", credentials.domainKey());
        assertEquals("api", credentials.apiKey());
        assertEquals("auth", credentials.authKey());
        assertEquals("PRODUCTION", credentials.environment());
    }

    @Test
    void settings_returnsSettingsView() {
        DiscoveryConfig config = new DiscoveryConfig(
                "acct", "domain", "api", "auth",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "price asc");

        DiscoverySettings settings = config.settings();

        assertEquals("https://core.dxpapi.com", settings.baseUri());
        assertEquals("https://pathways.dxpapi.com", settings.pathwaysBaseUri());
        assertEquals("https://suggest.dxpapi.com", settings.autosuggestBaseUri());
        assertEquals(12, settings.defaultPageSize());
        assertEquals("price asc", settings.defaultSort());
    }

    @Test
    void withCredentialOverrides_prefersNonBlankCredentialValues() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "jcr-api", "jcr-auth",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "price asc");
        DiscoveryConfig overrides = DiscoveryConfig.credentialsOnly(
                null, null, "env-api", "env-auth", "STAGING");

        DiscoveryConfig result = base.withCredentialOverrides(overrides);

        assertEquals("acct", result.accountId());
        assertEquals("domain", result.domainKey());
        assertEquals("env-api", result.apiKey());
        assertEquals("env-auth", result.authKey());
        assertEquals("STAGING", result.environment());
        assertEquals(ConfigDefaults.STAGING_BASE_URI, result.baseUri());
        assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, result.pathwaysBaseUri());
        assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, result.autosuggestBaseUri());
        assertEquals(12, result.defaultPageSize());
        assertEquals("price asc", result.defaultSort());
    }

    @Test
    void withCredentials_stagingEnvironmentOverride_updatesDerivedBaseUris() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "api", null,
                ConfigDefaults.BASE_URI, ConfigDefaults.PATHWAYS_BASE_URI, ConfigDefaults.AUTOSUGGEST_BASE_URI,
                "PRODUCTION", 12, "");
        DiscoveryCredentials stagingCreds = new DiscoveryCredentials(null, null, null, null, "STAGING");

        DiscoveryConfig result = base.withCredentials(stagingCreds);

        assertEquals("STAGING", result.environment());
        assertEquals(ConfigDefaults.STAGING_BASE_URI, result.baseUri());
        assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, result.pathwaysBaseUri());
        assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, result.autosuggestBaseUri());
    }

    @Test
    void withCredentials_preservesCustomUri_whenEnvironmentChangesToStaging() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "api", null,
                "https://custom.example.com", ConfigDefaults.PATHWAYS_BASE_URI, ConfigDefaults.AUTOSUGGEST_BASE_URI,
                "PRODUCTION", 12, "");
        DiscoveryCredentials stagingCreds = new DiscoveryCredentials(null, null, null, null, "STAGING");

        DiscoveryConfig result = base.withCredentials(stagingCreds);

        assertEquals("STAGING", result.environment());
        assertEquals("https://custom.example.com", result.baseUri());
        assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, result.pathwaysBaseUri());
        assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, result.autosuggestBaseUri());
    }

    @Test
    void withCredentials_noEnvironmentChange_urisUnchanged() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "old-api", null,
                ConfigDefaults.STAGING_BASE_URI, ConfigDefaults.STAGING_PATHWAYS_BASE_URI,
                ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, "STAGING", 12, "");
        DiscoveryCredentials updatedCreds = new DiscoveryCredentials("acct", "domain", "new-api", null, null);

        DiscoveryConfig result = base.withCredentials(updatedCreds);

        assertEquals("STAGING", result.environment());
        assertEquals(ConfigDefaults.STAGING_BASE_URI, result.baseUri());
        assertEquals(ConfigDefaults.STAGING_PATHWAYS_BASE_URI, result.pathwaysBaseUri());
        assertEquals(ConfigDefaults.STAGING_AUTOSUGGEST_BASE_URI, result.autosuggestBaseUri());
    }

    @Test
    void withCredentialOverrides_keepsBaseValuesWhenOverrideIsBlank() {
        DiscoveryConfig base = new DiscoveryConfig(
                "acct", "domain", "jcr-api", "jcr-auth",
                "https://core.dxpapi.com", "https://pathways.dxpapi.com", "https://suggest.dxpapi.com", "PRODUCTION",
                12, "price asc");
        DiscoveryConfig overrides = DiscoveryConfig.credentialsOnly(
                "", "  ", null, "", "");

        DiscoveryConfig result = base.withCredentialOverrides(overrides);

        assertEquals("acct", result.accountId());
        assertEquals("domain", result.domainKey());
        assertEquals("jcr-api", result.apiKey());
        assertEquals("jcr-auth", result.authKey());
        assertEquals("PRODUCTION", result.environment());
    }
}
