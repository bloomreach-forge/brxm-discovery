package org.bloomreach.forge.discovery.config.model;

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
        String defaultSort
) {
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
                settings.defaultSort()
        );
    }

    public static DiscoveryConfig credentialsOnly(String accountId, String domainKey,
                                                  String apiKey, String authKey,
                                                  String environment) {
        return new DiscoveryConfig(accountId, domainKey, apiKey, authKey,
                null, null, null, environment, 0, null);
    }

    public DiscoveryCredentials credentials() {
        return new DiscoveryCredentials(accountId, domainKey, apiKey, authKey, environment);
    }

    public DiscoverySettings settings() {
        return new DiscoverySettings(baseUri, pathwaysBaseUri, autosuggestBaseUri, defaultPageSize, defaultSort);
    }

    public DiscoveryConfig withCredentials(DiscoveryCredentials credentials) {
        return of(credentials, settings());
    }

    public DiscoveryConfig withCredentialOverrides(DiscoveryConfig overrides) {
        return overrides == null ? this : withCredentialOverrides(overrides.credentials());
    }

    public DiscoveryConfig withCredentialOverrides(DiscoveryCredentials overrides) {
        return withCredentials(credentials().withOverrides(overrides));
    }
}
