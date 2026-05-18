package org.bloomreach.forge.discovery.site.service.discovery;

import org.hippoecm.hst.core.component.HstRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Extracts client context (headers) and client IP address from an HST request.
 * All methods are pure functions; no state.
 */
public final class ClientContextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClientContextExtractor.class);
    private static final Pattern IP_PATTERN =
            Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]+$");

    private ClientContextExtractor() {
    }

    public static ClientContext clientContext(HstRequest request) {
        String ua = firstNonNull(request.getHeader("X-Forwarded-User-Agent"), request.getHeader("User-Agent"));
        String lang = firstNonNull(request.getHeader("X-Forwarded-Accept-Language"), request.getHeader("Accept-Language"));
        return new ClientContext(ua, lang, request.getHeader("X-Forwarded-For"));
    }

    private static String firstNonNull(String a, String b) {
        return a != null ? a : b;
    }

    public static String extractClientIp(HstRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String candidate = xff.split(",")[0].trim();
            if (IP_PATTERN.matcher(candidate).matches()) {
                if (!isLoopback(candidate)) return candidate;
                log.debug("Ignoring loopback X-Forwarded-For value: {}", candidate);
            } else {
                log.debug("Ignoring malformed X-Forwarded-For value: {}", candidate);
            }
        }
        String remoteAddr = request.getRemoteAddr();
        return (remoteAddr != null && !isLoopback(remoteAddr)) ? remoteAddr : "";
    }

    private static boolean isLoopback(String ip) {
        return "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("127.");
    }
}
