package org.bloomreach.forge.discovery.transport;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable descriptor for a single Discovery API HTTP call.
 * Headers must not include sensitive values in toString (callers own redaction).
 */
public record DiscoveryTransportRequest(URI uri, Map<String, String> headers, Duration timeout) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    public DiscoveryTransportRequest {
        Objects.requireNonNull(uri, "uri must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        headers = Map.copyOf(headers);
        timeout = timeout != null ? timeout : DEFAULT_TIMEOUT;
    }

    public static DiscoveryTransportRequest of(URI uri, Map<String, String> headers) {
        return new DiscoveryTransportRequest(uri, headers, null);
    }

    public static DiscoveryTransportRequest of(URI uri, Map<String, String> headers, Duration timeout) {
        return new DiscoveryTransportRequest(uri, headers, timeout);
    }
}
