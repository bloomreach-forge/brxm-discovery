package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.exception.DiscoveryException;

/**
 * Thin HTTP gateway used by all Discovery API clients.
 * Implementations are expected to be thread-safe singletons.
 */
public interface DiscoveryTransport {

    /**
     * Executes an HTTP GET for the given request and returns the raw response body.
     *
     * @throws DiscoveryException on non-2xx responses, I/O errors, or interruption
     */
    String execute(DiscoveryTransportRequest request);
}
