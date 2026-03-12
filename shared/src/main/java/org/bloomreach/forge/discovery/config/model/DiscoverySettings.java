package org.bloomreach.forge.discovery.config.model;

public record DiscoverySettings(
        String baseUri,
        String pathwaysBaseUri,
        int defaultPageSize,
        String defaultSort
) {
}
