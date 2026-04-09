package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

import static org.onehippo.cms7.services.HippoServiceRegistry.getService;

final class DiscoveryResourceExecutor {

    private volatile ResourceServiceBroker cachedBroker;

    /** Production: broker resolved lazily from HippoServiceRegistry on first use. */
    DiscoveryResourceExecutor() {}

    /** Test seam: inject broker directly so tests don't need HippoServiceRegistry. */
    DiscoveryResourceExecutor(ResourceServiceBroker broker) {
        this.cachedBroker = broker;
    }

    Resource resolve(String resourceSpace, String path, ClientContext ctx) throws ResourceException {
        return broker().resolve(resourceSpace, path, DiscoveryExchangeHints.buildHint(ctx));
    }

    Resource resolvePathways(String resourceSpace, String path, DiscoveryCredentials credentials,
                             ClientContext ctx) throws ResourceException {
        return broker().resolve(resourceSpace, path, DiscoveryExchangeHints.buildV2Hint(credentials, ctx));
    }

    private ResourceServiceBroker broker() {
        ResourceServiceBroker b = cachedBroker;
        if (b == null) {
            b = getService(ResourceServiceBroker.class);
            if (b == null) {
                throw new ConfigurationException(
                        "CRISP ResourceServiceBroker is not available in HippoServiceRegistry. " +
                        "Ensure crisp.broker.registerService=true is set in hst-config.properties.");
            }
            cachedBroker = b;
        }
        return b;
    }
}
