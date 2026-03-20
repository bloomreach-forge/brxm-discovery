package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryRuntimeContextFactoryTest {

    @Mock DiscoveryConfigProvider configProvider;
    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock ResolvedMount resolvedMount;
    @Mock Mount mount;
    @Mock jakarta.servlet.http.HttpServletRequest servletRequest;

    private final Map<String, Object> attrs = new HashMap<>();
    private DiscoveryRuntimeContextFactory factory;

    @BeforeEach
    void setUp() {
        factory = new DiscoveryRuntimeContextFactory(configProvider);

        DiscoveryConfig config = new DiscoveryConfig(
                "acct", "domain", "key", null,
                "https://core.dxpapi.com", "https://pathways.dxpapi.com",
                "https://suggest.dxpapi.com", "PRODUCTION", 10, "");

        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.getResolvedMount()).thenReturn(resolvedMount);
        lenient().when(resolvedMount.getMount()).thenReturn(mount);
        lenient().when(mount.getChannelInfo()).thenReturn(null);
        lenient().when(configProvider.get(nullable(Session.class))).thenReturn(config);
        lenient().doAnswer(inv -> attrs.get((String) inv.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(inv -> { attrs.put((String) inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(requestContext).setAttribute(anyString(), any());

        lenient().when(request.getCookies()).thenReturn(null);
        lenient().when(request.getScheme()).thenReturn("https");
        lenient().when(request.getServerName()).thenReturn("example.com");
        lenient().when(request.getServerPort()).thenReturn(443);
        lenient().when(request.getRequestURI()).thenReturn("/search");
        lenient().when(request.getQueryString()).thenReturn(null);
        lenient().when(request.getHeader("Referer")).thenReturn(null);
        lenient().when(request.getHeader("User-Agent")).thenReturn(null);
        lenient().when(request.getHeader("Accept-Language")).thenReturn(null);
        lenient().when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        lenient().when(requestContext.getServletRequest()).thenReturn(servletRequest);
        lenient().when(servletRequest.getParameter(anyString())).thenReturn(null);
        lenient().when(servletRequest.getParameterMap()).thenReturn(Map.of());
    }

    // ── X-Forwarded-For IP extraction (Part 1D) ───────────────────────────────

    @Test
    void validIpv4InXff_usedAsClientIp() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5");

        assertEquals("203.0.113.5", factory.get(request).clientIp());
    }

    @Test
    void multipleIpsInXff_firstOneUsed() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.2, 172.16.0.1");

        assertEquals("203.0.113.5", factory.get(request).clientIp());
    }

    @Test
    void malformedXff_fallsBackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("not-an-ip-address");

        assertEquals("10.0.0.1", factory.get(request).clientIp());
    }

    @Test
    void xffWithInjectionAttempt_fallsBackToRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn("'; DROP TABLE pixels;--");

        assertEquals("10.0.0.1", factory.get(request).clientIp());
    }

    @Test
    void nullXff_usesRemoteAddr() {
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);

        assertEquals("10.0.0.1", factory.get(request).clientIp());
    }
}
