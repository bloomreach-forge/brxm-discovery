package org.bloomreach.forge.discovery.site.service.discovery.config;

/**
 * Coded defaults for structural (non-sensitive) Discovery configuration.
 * Used as fallbacks when a JCR property is absent.
 */
public final class ConfigDefaults {

    private ConfigDefaults() {}

    public static final String BASE_URI = "https://core.dxpapi.com";
    public static final String PATHWAYS_BASE_URI = "https://pathways.dxpapi.com";
    public static final String ENVIRONMENT = "PRODUCTION";
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final String DEFAULT_SORT = "";
}
