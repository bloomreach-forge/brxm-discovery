package org.bloomreach.forge.discovery.site.service.discovery;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

public final class DiscoveryRequestLogging {

    private static final Set<String> SENSITIVE_PARAMS = Set.of("auth_key", "api_key");

    private DiscoveryRequestLogging() {
    }

    public static RequestLogContext requestLog(String path) {
        return new RequestLogContext(requestId(path), redactPath(path));
    }

    public static String requestId(String path) {
        return queryParam(path, "request_id").orElse("n/a");
    }

    public static String redactPath(String path) {
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

    private static Optional<String> queryParam(String path, String name) {
        int queryStart = path.indexOf('?');
        if (queryStart < 0) return Optional.empty();
        String query = path.substring(queryStart + 1);
        return Arrays.stream(query.split("&", -1))
                .filter(p -> p.startsWith(name + "="))
                .map(p -> p.substring(name.length() + 1))
                .findFirst();
    }

    public record RequestLogContext(String requestId, String redactedPath) {
    }
}
