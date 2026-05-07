package org.bloomreach.forge.discovery.site.service.discovery;

import org.hippoecm.hst.core.component.HstRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientContextExtractorTest {

    @Mock HstRequest request;

    // ── clientContext ─────────────────────────────────────────────────────

    @Test
    void clientContext_allHeadersPresent_returnsAll() {
        when(request.getHeader("X-Forwarded-User-Agent")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Forwarded-Accept-Language")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn("en-US");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        ClientContext ctx = ClientContextExtractor.clientContext(request);

        assertEquals("Mozilla/5.0", ctx.userAgent());
        assertEquals("en-US", ctx.acceptLanguage());
        assertEquals("1.2.3.4", ctx.xForwardedFor());
    }

    @Test
    void clientContext_nullHeaders_returnsNullFields() {
        when(request.getHeader("X-Forwarded-User-Agent")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getHeader("X-Forwarded-Accept-Language")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        ClientContext ctx = ClientContextExtractor.clientContext(request);

        assertNull(ctx.userAgent());
        assertNull(ctx.acceptLanguage());
        assertNull(ctx.xForwardedFor());
    }

    @Test
    void clientContext_xForwardedUserAgentTakesPrecedenceOverUserAgent() {
        when(request.getHeader("X-Forwarded-User-Agent")).thenReturn("Mozilla/5.0 (browser)");
        when(request.getHeader("User-Agent")).thenReturn("axios/1.13.5");
        when(request.getHeader("X-Forwarded-Accept-Language")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        ClientContext ctx = ClientContextExtractor.clientContext(request);

        assertEquals("Mozilla/5.0 (browser)", ctx.userAgent());
    }

    @Test
    void clientContext_xForwardedUserAgentAbsent_fallsBackToUserAgent() {
        when(request.getHeader("X-Forwarded-User-Agent")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(request.getHeader("X-Forwarded-Accept-Language")).thenReturn(null);
        when(request.getHeader("Accept-Language")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        ClientContext ctx = ClientContextExtractor.clientContext(request);

        assertEquals("Mozilla/5.0", ctx.userAgent());
    }

    @Test
    void clientContext_xForwardedAcceptLanguageTakesPrecedence() {
        when(request.getHeader("X-Forwarded-User-Agent")).thenReturn(null);
        when(request.getHeader("User-Agent")).thenReturn(null);
        when(request.getHeader("X-Forwarded-Accept-Language")).thenReturn("fr-FR");
        when(request.getHeader("Accept-Language")).thenReturn("en-US");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        ClientContext ctx = ClientContextExtractor.clientContext(request);

        assertEquals("fr-FR", ctx.acceptLanguage());
    }

    // ── extractClientIp ───────────────────────────────────────────────────

    @Test
    void extractClientIp_singleXff_returnsIt() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        assertEquals("1.2.3.4", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_multipleXff_returnsFirstEntry() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4, 5.6.7.8, 9.10.11.12");

        assertEquals("1.2.3.4", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_malformedXff_fallsBackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip");
        when(request.getRemoteAddr()).thenReturn("5.6.7.8");

        assertEquals("5.6.7.8", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_noXff_usesRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        assertEquals("10.0.0.1", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_blankXff_usesRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        assertEquals("10.0.0.1", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_nullRemoteAddr_returnsEmptyString() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn(null);

        assertEquals("", ClientContextExtractor.extractClientIp(request));
    }

    @Test
    void extractClientIp_ipv6Address_accepted() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8::1");

        assertEquals("2001:db8::1", ClientContextExtractor.extractClientIp(request));
    }
}
