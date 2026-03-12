package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;
import org.bloomreach.forge.discovery.site.service.discovery.ClientContext;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.bloomreach.forge.discovery.site.service.discovery.search.QueryParamParser;

final class DiscoveryRuntimeContext {

    private final DiscoveryConfig config;
    private final ClientContext clientContext;
    private final PixelFlags pixelFlags;
    private final QueryParamParser.RequestParamProvider paramProvider;
    private final String brUid2;
    private final String pageUrl;
    private final String refUrl;
    private final String clientIp;

    DiscoveryRuntimeContext(DiscoveryConfig config,
                            ClientContext clientContext,
                            PixelFlags pixelFlags,
                            QueryParamParser.RequestParamProvider paramProvider,
                            String brUid2,
                            String pageUrl,
                            String refUrl,
                            String clientIp) {
        this.config = config;
        this.clientContext = clientContext;
        this.pixelFlags = pixelFlags;
        this.paramProvider = paramProvider;
        this.brUid2 = brUid2;
        this.pageUrl = pageUrl;
        this.refUrl = refUrl;
        this.clientIp = clientIp;
    }

    DiscoveryConfig config() {
        return config;
    }

    DiscoveryCredentials credentials() {
        return config.credentials();
    }

    DiscoverySettings settings() {
        return config.settings();
    }

    ClientContext clientContext() {
        return clientContext;
    }

    PixelFlags pixelFlags() {
        return pixelFlags;
    }

    QueryParamParser.RequestParamProvider paramProvider() {
        return paramProvider;
    }

    String brUid2() {
        return brUid2;
    }

    String pageUrl() {
        return pageUrl;
    }

    String refUrl() {
        return refUrl;
    }

    String clientIp() {
        return clientIp;
    }
}
