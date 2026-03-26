package org.bloomreach.forge.discovery.config;

/**
 * Coded defaults for structural (non-sensitive) Discovery configuration.
 * Used as fallbacks when a JCR property is absent.
 */
public final class ConfigDefaults {

    private ConfigDefaults() {}

    public static final String CONFIG_NODE_PATH =
            "/hippo:configuration/hippo:modules/brxm-discovery/hippo:moduleconfig/discoveryConfig";

    public static final String ACCOUNT_ID_JCR = "brxdis:accountId";
    public static final String DOMAIN_KEY_JCR = "brxdis:domainKey";
    public static final String API_KEY_JCR = "brxdis:apiKey";
    public static final String AUTH_KEY_JCR = "brxdis:authKey";
    public static final String ENVIRONMENT_JCR = "brxdis:environment";
    public static final String BASE_URI_JCR = "brxdis:baseUri";
    public static final String PATHWAYS_BASE_URI_JCR = "brxdis:pathwaysBaseUri";
    public static final String AUTOSUGGEST_BASE_URI_JCR = "brxdis:autosuggestBaseUri";
    public static final String DEFAULT_PAGE_SIZE_JCR = "brxdis:defaultPageSize";
    public static final String DEFAULT_SORT_JCR = "brxdis:defaultSort";

    public static final String ACCOUNT_ID_SYS = "brxdis.accountId";
    public static final String DOMAIN_KEY_SYS = "brxdis.domainKey";
    public static final String API_KEY_SYS = "brxdis.apiKey";
    public static final String AUTH_KEY_SYS = "brxdis.authKey";
    public static final String ENVIRONMENT_SYS = "brxdis.environment";

    public static final String ACCOUNT_ID_ENV = "BRXDIS_ACCOUNT_ID";
    public static final String DOMAIN_KEY_ENV = "BRXDIS_DOMAIN_KEY";
    public static final String API_KEY_ENV = "BRXDIS_API_KEY";
    public static final String AUTH_KEY_ENV = "BRXDIS_AUTH_KEY";
    public static final String ENVIRONMENT_ENV = "BRXDIS_ENVIRONMENT";

    public static final String BASE_URI = "https://core.dxpapi.com";
    public static final String STAGING_BASE_URI = "https://staging-core.dxpapi.com";
    public static final String PATHWAYS_BASE_URI = "https://pathways.dxpapi.com";
    public static final String STAGING_PATHWAYS_BASE_URI = "https://staging-pathways.dxpapi.com";
    public static final String AUTOSUGGEST_BASE_URI = "https://suggest.dxpapi.com";
    public static final String STAGING_AUTOSUGGEST_BASE_URI = "https://staging-suggest.dxpapi.com";
    public static final String ENVIRONMENT = "PRODUCTION";
    public static final String STAGING_ENVIRONMENT = "STAGING";
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final String DEFAULT_SORT = "";

    public static boolean isStaging(String environment) {
        return STAGING_ENVIRONMENT.equalsIgnoreCase(environment);
    }

    /**
     * Resolves the effective base URI for the given environment.
     * Null, or either known default (production or staging), defers to the active environment.
     * A custom (non-default) URI is returned unchanged — enables private API endpoints.
     */
    public static String resolveBaseUri(String uri, String environment) {
        if (uri == null || uri.equals(BASE_URI) || uri.equals(STAGING_BASE_URI)) {
            return isStaging(environment) ? STAGING_BASE_URI : BASE_URI;
        }
        return uri;
    }

    public static String resolvePathwaysBaseUri(String uri, String environment) {
        if (uri == null || uri.equals(PATHWAYS_BASE_URI) || uri.equals(STAGING_PATHWAYS_BASE_URI)) {
            return isStaging(environment) ? STAGING_PATHWAYS_BASE_URI : PATHWAYS_BASE_URI;
        }
        return uri;
    }

    public static String resolveAutosuggestBaseUri(String uri, String environment) {
        if (uri == null || uri.equals(AUTOSUGGEST_BASE_URI) || uri.equals(STAGING_AUTOSUGGEST_BASE_URI)) {
            return isStaging(environment) ? STAGING_AUTOSUGGEST_BASE_URI : AUTOSUGGEST_BASE_URI;
        }
        return uri;
    }
}
