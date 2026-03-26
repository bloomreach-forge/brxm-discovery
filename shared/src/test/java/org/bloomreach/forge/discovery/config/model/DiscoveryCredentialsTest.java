package org.bloomreach.forge.discovery.config.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscoveryCredentialsTest {

    @Test
    void withOverrides_prefersNonBlankValues() {
        DiscoveryCredentials base = new DiscoveryCredentials(
                "acct", "domain", "jcr-api", "jcr-auth", "PRODUCTION");
        DiscoveryCredentials overrides = new DiscoveryCredentials(
                null, "override-domain", "env-api", "", "STAGING");

        DiscoveryCredentials result = base.withOverrides(overrides);

        assertEquals("acct", result.accountId());
        assertEquals("override-domain", result.domainKey());
        assertEquals("env-api", result.apiKey());
        assertEquals("jcr-auth", result.authKey());
        assertEquals("STAGING", result.environment());
    }

    @Test
    void hasSearchCredentials_requiresAccountDomainAndApiKey() {
        assertTrue(new DiscoveryCredentials("acct", "domain", "api", null, "PRODUCTION").hasSearchCredentials());
        assertFalse(new DiscoveryCredentials("acct", "domain", null, null, "PRODUCTION").hasSearchCredentials());
    }

    @Test
    void hasPathwaysCredentials_requiresAuthKeyInAdditionToSearchCredentials() {
        assertTrue(new DiscoveryCredentials("acct", "domain", "api", "auth", "PRODUCTION").hasPathwaysCredentials());
        assertFalse(new DiscoveryCredentials("acct", "domain", "api", null, "PRODUCTION").hasPathwaysCredentials());
    }
}
