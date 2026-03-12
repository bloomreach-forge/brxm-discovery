package org.bloomreach.forge.discovery.site.service.discovery;

final class DiscoveryRequestLogging {

    private DiscoveryRequestLogging() {
    }

    static RequestLogContext requestLog(String path) {
        return new RequestLogContext(requestId(path), redactPath(path));
    }

    static String requestId(String path) {
        int start = path.indexOf("request_id=");
        if (start < 0) {
            return "n/a";
        }
        int valueStart = start + "request_id=".length();
        int end = path.indexOf('&', valueStart);
        return end < 0 ? path.substring(valueStart) : path.substring(valueStart, end);
    }

    static String redactPath(String path) {
        int start = path.indexOf("auth_key=");
        if (start < 0) {
            return path;
        }
        int valueStart = start + "auth_key=".length();
        int end = path.indexOf('&', valueStart);
        if (end < 0) {
            end = path.length();
        }
        return path.substring(0, valueStart) + "***" + path.substring(end);
    }

    record RequestLogContext(String requestId, String redactedPath) {
    }
}
