package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.request.DiscoveryRequestSpec;
import org.springframework.web.util.UriComponentsBuilder;

final class DiscoveryRequestPaths {

    private DiscoveryRequestPaths() {
    }

    static String toRelativePath(DiscoveryRequestSpec request) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(request.path());
        request.forEachQueryParameter((name, value) -> builder.queryParam(name, value));
        return builder.build(false).toUriString();
    }
}
