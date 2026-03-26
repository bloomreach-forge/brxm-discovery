package org.bloomreach.forge.discovery.site.service.discovery;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DiscoveryRequestLoggingTest {

    // ── redactPath ────────────────────────────────────────────────────────────

    @Test
    void redactPath_noQueryString_returnsUnchanged() {
        assertEquals("https://example.com/api/v1/core/",
                DiscoveryRequestLogging.redactPath("https://example.com/api/v1/core/"));
    }

    @Test
    void redactPath_authKey_redacted() {
        String path = "https://example.com/api?account_id=acct&auth_key=secret123&q=shoes";
        assertEquals("https://example.com/api?account_id=acct&auth_key=***&q=shoes",
                DiscoveryRequestLogging.redactPath(path));
    }

    @Test
    void redactPath_apiKey_redacted() {
        String path = "https://example.com/api?account_id=acct&api_key=myapikey&q=shoes";
        assertEquals("https://example.com/api?account_id=acct&api_key=***&q=shoes",
                DiscoveryRequestLogging.redactPath(path));
    }

    @Test
    void redactPath_bothAuthKeyAndApiKey_bothRedacted() {
        String path = "https://example.com/api?auth_key=secret&api_key=other&q=boots";
        assertEquals("https://example.com/api?auth_key=***&api_key=***&q=boots",
                DiscoveryRequestLogging.redactPath(path));
    }

    @Test
    void redactPath_noSensitiveParams_returnsUnchanged() {
        String path = "https://example.com/api?account_id=acct&q=shoes&rows=10";
        assertEquals(path, DiscoveryRequestLogging.redactPath(path));
    }

    @Test
    void redactPath_partialMatch_notRedacted() {
        // "auth_keys" is not "auth_key" — must not match
        String path = "https://example.com/api?auth_keys=should-not-redact&q=test";
        assertEquals(path, DiscoveryRequestLogging.redactPath(path));
    }

    // ── requestId ─────────────────────────────────────────────────────────────

    @Test
    void requestId_extractsFromPath() {
        String path = "https://example.com/api?q=shoes&request_id=abc-123&rows=10";
        assertEquals("abc-123", DiscoveryRequestLogging.requestId(path));
    }

    @Test
    void requestId_missing_returnsNA() {
        String path = "https://example.com/api?q=shoes";
        assertEquals("n/a", DiscoveryRequestLogging.requestId(path));
    }
}
