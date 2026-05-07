package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class PixelUserAgentPolicy {

    private static final int DEDUP_MAX = 1000;
    private static final Set<String> WARNED_UAS = ConcurrentHashMap.newKeySet();

    private static final String[] BLOCKLIST = {
            "axios", "node-fetch", "python-requests", "java/", "curl", "wget",
            "postmanruntime", "bot", "spider", "crawler"
    };

    private PixelUserAgentPolicy() {}

    static boolean isAllowed(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String lower = userAgent.toLowerCase(Locale.ROOT);
        for (String blocked : BLOCKLIST) {
            if (lower.contains(blocked)) {
                return false;
            }
        }
        return true;
    }

    /** Returns true the first time a given UA is seen (up to DEDUP_MAX distinct UAs). */
    static boolean shouldWarn(String userAgent) {
        if (userAgent == null) return false;
        if (WARNED_UAS.size() < DEDUP_MAX) {
            return WARNED_UAS.add(userAgent);
        }
        return false;
    }
}
