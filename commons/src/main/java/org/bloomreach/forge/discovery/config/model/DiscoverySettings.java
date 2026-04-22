package org.bloomreach.forge.discovery.config.model;

public record DiscoverySettings(
        String baseUri,
        String pathwaysBaseUri,
        String autosuggestBaseUri,
        int defaultPageSize,
        String defaultSort,
        DiscoverySchemaConfig schemaConfig
) {
    public DiscoverySettings {
        schemaConfig = schemaConfig != null ? schemaConfig : DiscoverySchemaConfig.DEFAULT;
    }

    /** Backward-compatible constructor for callers that don't supply schemaConfig. */
    public DiscoverySettings(String baseUri, String pathwaysBaseUri, String autosuggestBaseUri,
                             int defaultPageSize, String defaultSort) {
        this(baseUri, pathwaysBaseUri, autosuggestBaseUri, defaultPageSize, defaultSort, null);
    }

    public DiscoverySettings withSchemaConfig(DiscoverySchemaConfig newSchemaConfig) {
        return new DiscoverySettings(baseUri, pathwaysBaseUri, autosuggestBaseUri,
                defaultPageSize, defaultSort, newSchemaConfig);
    }
}
