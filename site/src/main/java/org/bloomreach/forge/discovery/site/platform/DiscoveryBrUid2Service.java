package org.bloomreach.forge.discovery.site.platform;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class DiscoveryBrUid2Service {

    private static final String ATTR = DiscoveryBrUid2Service.class.getName() + ".value";
    private static final Duration COOKIE_TTL = Duration.ofDays(365L * 2);

    public String ensure(HstRequest request) {
        Object cached = attribute(request);
        if (cached instanceof String value && !value.isBlank()) {
            return value;
        }

        String existing = cookieValue(request, HstDiscoveryService.BR_UID_2);
        if (existing != null && !existing.isBlank()) {
            String normalized = DiscoveryBrUid2.promoteReturningVisitor(existing);
            cache(request, normalized);
            if (!normalized.equals(existing)) {
                addCookie(request, normalized, isSecure(request));
            }
            return normalized;
        }

        String generated = DiscoveryBrUid2.generateEncoded();
        cache(request, generated);
        addCookie(request, generated, isSecure(request));
        return generated;
    }

    public String resolve(HstRequest request) {
        Object cached = attribute(request);
        if (cached instanceof String value && !value.isBlank()) {
            return value;
        }
        return cookieValue(request, HstDiscoveryService.BR_UID_2);
    }

    private static Object attribute(HstRequest request) {
        HstRequestContext requestContext = request.getRequestContext();
        return requestContext != null ? requestContext.getAttribute(ATTR) : null;
    }

    private static void cache(HstRequest request, String value) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext != null) {
            requestContext.setAttribute(ATTR, value);
        }
    }

    private static void addCookie(HstRequest request, String value, boolean secure) {
        HstRequestContext requestContext = request.getRequestContext();
        if (requestContext == null) {
            return;
        }
        HttpServletResponse response = requestContext.getServletResponse();
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(HstDiscoveryService.BR_UID_2, value)
                .path("/")
                .maxAge(COOKIE_TTL)
                .httpOnly(false)
                .secure(secure)
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    private static boolean isSecure(HstRequest request) {
        return "https".equalsIgnoreCase(request.getScheme());
    }

    private static String cookieValue(HstRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
