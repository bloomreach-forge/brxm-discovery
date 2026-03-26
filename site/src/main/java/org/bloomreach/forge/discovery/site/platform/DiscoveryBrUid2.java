package org.bloomreach.forge.discovery.site.platform;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

final class DiscoveryBrUid2 {

    private static final String VERSION_MARKER = ":v=app:";
    private static final String FIRST_HIT_MARKER = ":hc=1";
    private static final String RETURNING_HIT_MARKER = ":hc=2";

    private DiscoveryBrUid2() {
    }

    static String generateEncoded() {
        String raw = "uid=" + UUID.randomUUID().toString().replace("-", "")
                + ":v=app:ts=0:hc=1";
        return encode(raw);
    }

    static String promoteReturningVisitor(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return encoded;
        }
        try {
            String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            if (!decoded.contains(VERSION_MARKER) || !decoded.endsWith(FIRST_HIT_MARKER)) {
                return encoded;
            }
            return encode(decoded.substring(0, decoded.length() - FIRST_HIT_MARKER.length()) + RETURNING_HIT_MARKER);
        } catch (IllegalArgumentException e) {
            return encoded;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
