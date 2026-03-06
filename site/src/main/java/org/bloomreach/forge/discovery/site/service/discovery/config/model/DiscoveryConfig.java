package org.bloomreach.forge.discovery.site.service.discovery.config.model;

public record DiscoveryConfig(
        String accountId,
        String domainKey,
        String apiKey,
        String authKey,
        String baseUri,
        String pathwaysBaseUri,
        String environment,
        int defaultPageSize,
        String defaultSort
) {
}
