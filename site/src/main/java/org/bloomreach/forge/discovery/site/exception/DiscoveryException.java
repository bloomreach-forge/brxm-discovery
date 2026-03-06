package org.bloomreach.forge.discovery.site.exception;

public sealed class DiscoveryException extends RuntimeException
        permits SearchException, RecommendationException, ConfigurationException {

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
