package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelUserAgentPolicyTest {

    // ── blocked UAs ───────────────────────────────────────────────────────

    @Test void axios_blocked()            { assertFalse(PixelUserAgentPolicy.isAllowed("axios/1.13.5")); }
    @Test void nodeFetch_blocked()        { assertFalse(PixelUserAgentPolicy.isAllowed("node-fetch/3.0.0")); }
    @Test void pythonRequests_blocked()   { assertFalse(PixelUserAgentPolicy.isAllowed("python-requests/2.28.1")); }
    @Test void java_blocked()             { assertFalse(PixelUserAgentPolicy.isAllowed("java/17.0.5")); }
    @Test void curl_blocked()             { assertFalse(PixelUserAgentPolicy.isAllowed("curl/7.85.0")); }
    @Test void wget_blocked()             { assertFalse(PixelUserAgentPolicy.isAllowed("Wget/1.21")); }
    @Test void postman_blocked()          { assertFalse(PixelUserAgentPolicy.isAllowed("PostmanRuntime/7.29.0")); }
    @Test void googlebot_blocked()        { assertFalse(PixelUserAgentPolicy.isAllowed("Googlebot/2.1 (+http://www.google.com/bot.html)")); }
    @Test void spider_blocked()           { assertFalse(PixelUserAgentPolicy.isAllowed("MyScraper/1.0 (spider)")); }
    @Test void crawler_blocked()          { assertFalse(PixelUserAgentPolicy.isAllowed("MyCrawler/1.0")); }

    // ── allowed UAs ───────────────────────────────────────────────────────

    @Test
    void macChromeBrowser_allowed() {
        assertTrue(PixelUserAgentPolicy.isAllowed(
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36"));
    }

    @Test
    void windowsBrowser_allowed() {
        assertTrue(PixelUserAgentPolicy.isAllowed(
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"));
    }

    @Test
    void iPhoneBrowser_allowed() {
        assertTrue(PixelUserAgentPolicy.isAllowed(
                "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15"));
    }

    @Test
    void androidBrowser_allowed() {
        assertTrue(PixelUserAgentPolicy.isAllowed(
                "Mozilla/5.0 (Linux; Android 12; Pixel 6) AppleWebKit/537.36"));
    }

    // ── edge cases ────────────────────────────────────────────────────────

    @Test void nullUA_allowed()  { assertTrue(PixelUserAgentPolicy.isAllowed(null)); }
    @Test void blankUA_allowed() { assertTrue(PixelUserAgentPolicy.isAllowed("   ")); }

    @Test
    void blockedUAUpperCase_blocked() {
        assertFalse(PixelUserAgentPolicy.isAllowed("CURL/7.85.0"));
    }

    @Test
    void blockedUAMixedCase_blocked() {
        assertFalse(PixelUserAgentPolicy.isAllowed("Axios/1.0.0"));
    }

    // ── shouldWarn dedup ──────────────────────────────────────────────────

    @Test void shouldWarn_nullUA_returnsFalse() { assertFalse(PixelUserAgentPolicy.shouldWarn(null)); }

    @Test
    void shouldWarn_newUA_returnsTrue() {
        assertTrue(PixelUserAgentPolicy.shouldWarn("TestBlockedUA-" + System.nanoTime()));
    }

    @Test
    void shouldWarn_sameUASecondTime_returnsFalse() {
        String ua = "TestBlockedUA-dedup-" + System.nanoTime();
        PixelUserAgentPolicy.shouldWarn(ua);
        assertFalse(PixelUserAgentPolicy.shouldWarn(ua));
    }
}
