package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.services.HippoServiceRegistry;

/**
 * Resolves the CRISP broker from the active HST component manager at bean-creation time.
 *
 * <p>The site overrides assembly cannot reliably reference the CRISP addon broker bean
 * directly by id because addon contexts and the main site context do not share the same
 * bean namespace. This factory keeps the client constructor-injected while resolving the
 * broker through the runtime component manager that actually owns it.
 */
public class CrispResourceServiceBrokerFactory {

    public ResourceServiceBroker getObject() {
        ResourceServiceBroker broker = lookupHstBroker();
        if (broker == null) {
            broker = HippoServiceRegistry.getService(ResourceServiceBroker.class);
        }
        if (broker == null) {
            throw new ConfigurationException(
                    "CRISP ResourceServiceBroker not found in the HST component manager or HippoServiceRegistry. " +
                    "Ensure the CRISP HST addon is present in the site webapp and, for cross-webapp sharing, " +
                    "crisp.broker.registerService=true is set in the relevant hst-config.properties.");
        }
        return broker;
    }

    static ResourceServiceBroker lookupHstBroker() {
        if (!HstServices.isAvailable()) {
            return null;
        }
        try {
            return ResourceServiceBroker.class.cast(
                    HstServices.getComponentManager().getComponent(ResourceServiceBroker.class.getName()));
        } catch (RuntimeException e) {
            return null;
        }
    }
}
