package org.bloomreach.forge.discovery.config.model;

public record DiscoverySettings(
        String baseUri,
        String pathwaysBaseUri,
        String autosuggestBaseUri,
        int defaultPageSize,
        String defaultSort,
        DiscoverySchemaConfig schemaConfig,
        String pixelBaseUri,
        String pixelBaseUriEU
) {
    private static final String DEFAULT_PIXEL_BASE_URI    = "https://p.brsrvr.com";
    private static final String DEFAULT_PIXEL_BASE_URI_EU = "https://p-eu.brsrvr.com";

    public DiscoverySettings {
        schemaConfig   = schemaConfig   != null ? schemaConfig   : DiscoverySchemaConfig.DEFAULT;
        pixelBaseUri   = pixelBaseUri   != null ? pixelBaseUri   : DEFAULT_PIXEL_BASE_URI;
        pixelBaseUriEU = pixelBaseUriEU != null ? pixelBaseUriEU : DEFAULT_PIXEL_BASE_URI_EU;
    }

    /** Backward-compatible constructor for callers that don't supply schemaConfig or pixel URIs. */
    public DiscoverySettings(String baseUri, String pathwaysBaseUri, String autosuggestBaseUri,
                             int defaultPageSize, String defaultSort) {
        this(baseUri, pathwaysBaseUri, autosuggestBaseUri, defaultPageSize, defaultSort, null, null, null);
    }

    /** Backward-compatible constructor for callers that supply schemaConfig but not pixel URIs. */
    public DiscoverySettings(String baseUri, String pathwaysBaseUri, String autosuggestBaseUri,
                             int defaultPageSize, String defaultSort, DiscoverySchemaConfig schemaConfig) {
        this(baseUri, pathwaysBaseUri, autosuggestBaseUri, defaultPageSize, defaultSort, schemaConfig, null, null);
    }

    public DiscoverySettings withSchemaConfig(DiscoverySchemaConfig newSchemaConfig) {
        return new DiscoverySettings(baseUri, pathwaysBaseUri, autosuggestBaseUri,
                defaultPageSize, defaultSort, newSchemaConfig, pixelBaseUri, pixelBaseUriEU);
    }
}
