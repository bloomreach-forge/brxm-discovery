package org.bloomreach.forge.discovery.config.model;

public record DiscoverySettings(
        String baseUri,
        String pathwaysBaseUri,
        String autosuggestBaseUri,
        int defaultPageSize,
        String defaultSort
) {
}
