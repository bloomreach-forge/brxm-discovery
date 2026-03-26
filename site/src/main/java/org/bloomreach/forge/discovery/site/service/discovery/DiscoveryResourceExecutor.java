package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

final class DiscoveryResourceExecutor {

    private final ResourceServiceBroker broker;

    DiscoveryResourceExecutor(ResourceServiceBroker broker) {
        this.broker = broker;
    }

    Resource resolve(String resourceSpace, String path, ClientContext ctx) throws ResourceException {
        return broker.resolve(resourceSpace, path, DiscoveryExchangeHints.buildHint(ctx));
    }

    Resource resolvePathways(String resourceSpace, String path, DiscoveryCredentials credentials,
                             ClientContext ctx) throws ResourceException {
        return broker.resolve(resourceSpace, path, DiscoveryExchangeHints.buildV2Hint(credentials, ctx));
    }
}
