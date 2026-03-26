package org.bloomreach.forge.discovery.site.service.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Set;

final class DiscoveryRequestLogging {

    private static final Set<String> SENSITIVE_PARAMS = Set.of("auth_key", "api_key");

    private DiscoveryRequestLogging() {
    }

    static RequestLogContext requestLog(String path) {
        return new RequestLogContext(requestId(path), redactPath(path));
    }

    static String requestId(String path) {
        return queryParam(path, "request_id").orElse("n/a");
    }

    static String redactPath(String path) {
        int queryStart = path.indexOf('?');
        if (queryStart < 0) {
            return path;
        }
        String base = path.substring(0, queryStart + 1);
        String query = path.substring(queryStart + 1);
        String redacted = Arrays.stream(query.split("&", -1))
                .map(DiscoveryRequestLogging::redactParam)
                .reduce((a, b) -> a + "&" + b)
                .orElse(query);
        return base + redacted;
    }

    private static String redactParam(String param) {
        int eq = param.indexOf('=');
        if (eq < 0) return param;
        String name = param.substring(0, eq);
        return SENSITIVE_PARAMS.contains(name) ? name + "=***" : param;
    }

    private static java.util.Optional<String> queryParam(String path, String name) {
        int queryStart = path.indexOf('?');
        if (queryStart < 0) return java.util.Optional.empty();
        String query = path.substring(queryStart + 1);
        return Arrays.stream(query.split("&", -1))
                .filter(p -> p.startsWith(name + "="))
                .map(p -> p.substring(name.length() + 1))
                .findFirst();
    }

    record RequestLogContext(String requestId, String redactedPath) {
    }
}
