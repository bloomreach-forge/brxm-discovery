package org.bloomreach.forge.discovery.crisp;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver;
import org.onehippo.cms7.services.HippoServiceRegistry;

import java.util.Objects;

/**
 * CRISP resolver that derives its base URI from the shared Discovery settings.
 * Supports direct Spring injection and HippoServiceRegistry lookup so the same
 * resolver can be reused by both site and platform CRISP contexts.
 */
public class ConfigBackedDiscoveryResourceResolver extends SimpleJacksonRestTemplateResourceResolver {

    private DiscoveryConfigProvider configProvider;
    private volatile DiscoveryConfigProvider resolvedProvider;
    private String api;

    public void setConfigProvider(DiscoveryConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    public void setApi(String api) {
        this.api = api;
    }

    @Override
    protected String getBaseResourceURI(String relPath) {
        String baseUri = resolveBaseUri();
        if (baseUri.endsWith("/") && relPath.startsWith("/")) {
            return baseUri + relPath.substring(1);
        }
        return baseUri + relPath;
    }

    private String resolveBaseUri() {
        DiscoverySettings settings = provider().settings();
        return switch (Objects.requireNonNull(api, "api")) {
            case "search" -> settings.baseUri();
            case "pathways" -> settings.pathwaysBaseUri();
            case "autosuggest" -> settings.autosuggestBaseUri();
            default -> throw new IllegalStateException("Unsupported Discovery resolver api: " + api);
        };
    }

    private DiscoveryConfigProvider provider() {
        if (configProvider != null) {
            return configProvider;
        }
        DiscoveryConfigProvider cached = resolvedProvider;
        if (cached != null) {
            return cached;
        }
        DiscoveryConfigProvider service = lookupConfigProvider();
        if (service == null) {
            throw new IllegalStateException("DiscoveryConfigProvider is not available via Spring or HippoServiceRegistry");
        }
        resolvedProvider = service;
        return service;
    }

    protected DiscoveryConfigProvider lookupConfigProvider() {
        return HippoServiceRegistry.getService(DiscoveryConfigProvider.class);
    }
}
