package org.bloomreach.forge.discovery.config.model;

import org.bloomreach.forge.discovery.config.ConfigDefaults;

public record DiscoveryConfig(
        String accountId,
        String domainKey,
        String apiKey,
        String authKey,
        String baseUri,
        String pathwaysBaseUri,
        String autosuggestBaseUri,
        String environment,
        int defaultPageSize,
        String defaultSort,
        DiscoverySchemaConfig schemaConfig
) {
    public DiscoveryConfig {
        schemaConfig = schemaConfig != null ? schemaConfig : DiscoverySchemaConfig.DEFAULT;
    }

    /** Backward-compatible constructor for callers that don't supply schemaConfig. */
    public DiscoveryConfig(String accountId, String domainKey, String apiKey, String authKey,
                           String baseUri, String pathwaysBaseUri, String autosuggestBaseUri,
                           String environment, int defaultPageSize, String defaultSort) {
        this(accountId, domainKey, apiKey, authKey,
                baseUri, pathwaysBaseUri, autosuggestBaseUri,
                environment, defaultPageSize, defaultSort, null);
    }

    public static DiscoveryConfig of(DiscoveryCredentials credentials, DiscoverySettings settings) {
        return new DiscoveryConfig(
                credentials.accountId(),
                credentials.domainKey(),
                credentials.apiKey(),
                credentials.authKey(),
                settings.baseUri(),
                settings.pathwaysBaseUri(),
                settings.autosuggestBaseUri(),
                credentials.environment(),
                settings.defaultPageSize(),
                settings.defaultSort(),
                settings.schemaConfig()
        );
    }

    public static DiscoveryConfig credentialsOnly(String accountId, String domainKey,
                                                  String apiKey, String authKey,
                                                  String environment) {
        return new DiscoveryConfig(accountId, domainKey, apiKey, authKey,
                null, null, null, environment, 0, null, null);
    }

    public DiscoveryCredentials credentials() {
        return new DiscoveryCredentials(accountId, domainKey, apiKey, authKey, environment);
    }

    public DiscoverySettings settings() {
        return new DiscoverySettings(baseUri, pathwaysBaseUri, autosuggestBaseUri, defaultPageSize, defaultSort, schemaConfig);
    }

    public DiscoveryConfig withCredentials(DiscoveryCredentials credentials) {
        String newEnv = (credentials.environment() != null && !credentials.environment().isBlank())
                ? credentials.environment() : this.environment;
        return new DiscoveryConfig(
                credentials.accountId(), credentials.domainKey(),
                credentials.apiKey(), credentials.authKey(),
                ConfigDefaults.resolveBaseUri(baseUri, newEnv),
                ConfigDefaults.resolvePathwaysBaseUri(pathwaysBaseUri, newEnv),
                ConfigDefaults.resolveAutosuggestBaseUri(autosuggestBaseUri, newEnv),
                newEnv, defaultPageSize, defaultSort, schemaConfig
        );
    }

    public DiscoveryConfig withFieldList(String fieldList) {
        return new DiscoveryConfig(accountId, domainKey, apiKey, authKey,
                baseUri, pathwaysBaseUri, autosuggestBaseUri, environment, defaultPageSize, defaultSort,
                schemaConfig.withDefaultFieldList(fieldList));
    }

    public DiscoveryConfig withCredentialOverrides(DiscoveryConfig overrides) {
        return overrides == null ? this : withCredentialOverrides(overrides.credentials());
    }

    public DiscoveryConfig withCredentialOverrides(DiscoveryCredentials overrides) {
        return withCredentials(credentials().withOverrides(overrides));
    }
}
