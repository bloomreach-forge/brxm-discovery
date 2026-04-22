package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;

record DiscoveryRuntimeContext(
        DiscoveryConfig config,
        ClientContext clientContext,
        PixelFlags pixelFlags,
        QueryParamParser.RequestParamProvider paramProvider,
        String brUid2,
        String pageUrl,
        String pageTitle,
        String refUrl,
        String origRefUrl,
        String clientIp
) {
    DiscoveryCredentials credentials() {
        return config.credentials();
    }

    DiscoverySettings settings() {
        return config.settings();
    }
}
