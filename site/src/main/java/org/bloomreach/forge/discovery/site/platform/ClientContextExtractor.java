package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.hippoecm.hst.core.component.HstRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Pattern;

/**
 * Extracts client context (headers) and client IP address from an HST request.
 * All methods are pure functions; no state.
 */
final class ClientContextExtractor {

    private static final Logger log = LoggerFactory.getLogger(ClientContextExtractor.class);
    private static final Pattern IP_PATTERN =
            Pattern.compile("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$|^[0-9a-fA-F:]+$");

    private ClientContextExtractor() {
    }

    static ClientContext clientContext(HstRequest request) {
        return new ClientContext(
                request.getHeader("User-Agent"),
                request.getHeader("Accept-Language"),
                request.getHeader("X-Forwarded-For")
        );
    }

    static String extractClientIp(HstRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String candidate = xff.split(",")[0].trim();
            if (IP_PATTERN.matcher(candidate).matches()) {
                return candidate;
            }
            log.debug("Ignoring malformed X-Forwarded-For value: {}", candidate);
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "";
    }
}
