package org.bloomreach.forge.discovery.config.model;

public record DiscoveryCredentials(
        String accountId,
        String domainKey,
        String apiKey,
        String authKey,
        String environment
) {
    public DiscoveryCredentials withOverrides(DiscoveryCredentials overrides) {
        if (overrides == null) {
            return this;
        }
        return new DiscoveryCredentials(
                nonBlankOr(overrides.accountId(), accountId),
                nonBlankOr(overrides.domainKey(), domainKey),
                nonBlankOr(overrides.apiKey(), apiKey),
                nonBlankOr(overrides.authKey(), authKey),
                nonBlankOr(overrides.environment(), environment)
        );
    }

    public boolean hasSearchCredentials() {
        return isPresent(accountId) && isPresent(domainKey) && isPresent(apiKey);
    }

    public boolean hasPathwaysCredentials() {
        return hasSearchCredentials() && isPresent(authKey);
    }

    private static String nonBlankOr(String preferred, String fallback) {
        return isPresent(preferred) ? preferred : fallback;
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
