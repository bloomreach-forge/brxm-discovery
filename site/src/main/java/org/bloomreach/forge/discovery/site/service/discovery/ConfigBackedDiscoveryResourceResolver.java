package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.onehippo.cms7.crisp.core.resource.jackson.SimpleJacksonRestTemplateResourceResolver;

import java.util.Objects;

/**
 * Site-side CRISP resolver that derives its base URI from the shared Discovery settings.
 * This keeps CRISP transport aligned with the same env/sys/JCR-resolved config used by CMS.
 */
public class ConfigBackedDiscoveryResourceResolver extends SimpleJacksonRestTemplateResourceResolver {

    private DiscoveryConfigProvider configProvider;
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
        DiscoverySettings settings = Objects.requireNonNull(configProvider, "configProvider")
                .get(null)
                .settings();
        return switch (Objects.requireNonNull(api, "api")) {
            case "search" -> settings.baseUri();
            case "pathways" -> settings.pathwaysBaseUri();
            case "autosuggest" -> settings.autosuggestBaseUri();
            default -> throw new IllegalStateException("Unsupported Discovery resolver api: " + api);
        };
    }
}
