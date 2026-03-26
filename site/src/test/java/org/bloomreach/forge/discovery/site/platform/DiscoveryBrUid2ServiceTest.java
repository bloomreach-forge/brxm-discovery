package org.bloomreach.forge.discovery.site.platform;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DiscoveryBrUid2ServiceTest {

    @Mock HstRequest request;
    @Mock HttpServletResponse response;
    @Mock HstRequestContext requestContext;

    private final Map<String, Object> attrs = new HashMap<>();
    private DiscoveryBrUid2Service service;

    @BeforeEach
    void setUp() {
        service = new DiscoveryBrUid2Service();
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().doAnswer(invocation -> attrs.get(invocation.getArgument(0)))
                .when(requestContext).getAttribute(anyString());
        lenient().doAnswer(invocation -> {
            attrs.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(requestContext).setAttribute(anyString(), any());
        lenient().when(request.getScheme()).thenReturn("https");
        lenient().when(requestContext.getServletResponse()).thenReturn(response);
    }

    @Test
    void ensure_missingCookie_generatesAndWritesCookie() {
        when(request.getCookies()).thenReturn(null);

        String value = service.ensure(request);

        assertNotNull(value);
        assertEquals(value, service.resolve(request));
        verify(response).addHeader(contains("Set-Cookie"), contains("_br_uid_2="));
        verify(response).addHeader(contains("Set-Cookie"), contains("SameSite=Lax"));
        verify(response).addHeader(contains("Set-Cookie"), contains("Secure"));
    }

    @Test
    void ensure_existingCookie_reusesValueWithoutRewriting() {
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("_br_uid_2", "uid%3Dfoo%3Av%3D15.0%3Ats%3D1%3Ahc%3D55")
        });

        String value = service.ensure(request);

        assertEquals("uid%3Dfoo%3Av%3D15.0%3Ats%3D1%3Ahc%3D55", value);
        verify(response, never()).addHeader(contains("Set-Cookie"), anyString());
    }

    @Test
    void ensure_pluginManagedFirstHit_promotesReturningVisitor() {
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("_br_uid_2", "uid%3Dabc%3Av%3Dapp%3Ats%3D0%3Ahc%3D1")
        });

        String value = service.ensure(request);

        assertTrue(value.endsWith("%3Ahc%3D2"));
        verify(response).addHeader(contains("Set-Cookie"), contains("hc%3D2"));
    }

    @Test
    void ensure_cachedOnRequest_onlyWritesOnceAcrossComponents() {
        when(request.getCookies()).thenReturn(null);

        String first = service.ensure(request);
        String second = service.ensure(request);

        assertEquals(first, second);
        verify(response).addHeader(contains("Set-Cookie"), contains("_br_uid_2="));
    }
}
