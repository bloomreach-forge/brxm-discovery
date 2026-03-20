package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.hippoecm.hst.core.container.ComponentsException;
import org.hippoecm.hst.site.HstServices;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the CRISP broker from the active HST component manager at bean-creation time.
 *
 * <p>The site overrides assembly cannot reliably reference the CRISP addon broker bean
 * directly by id because addon contexts and the main site context do not share the same
 * bean namespace. This factory keeps the client constructor-injected while resolving the
 * broker through the runtime component manager that actually owns it.
 */
public class CrispResourceServiceBrokerFactory {

    private static final Logger log = LoggerFactory.getLogger(CrispResourceServiceBrokerFactory.class);
    private static final String CRISP_HST_ADDON_MODULE = "org.onehippo.cms7.crisp.hst";

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
        ResourceServiceBroker broker = null;
        try {
            broker = HstServices.getComponentManager().getComponent(ResourceServiceBroker.class);
        } catch (ComponentsException e) {
            log.debug("Typed broker lookup failed in main context, trying by name: {}", e.getMessage());
            try {
                broker = ResourceServiceBroker.class.cast(
                        HstServices.getComponentManager().getComponent(ResourceServiceBroker.class.getName()));
            } catch (RuntimeException e2) {
                log.debug("Bean-name broker lookup failed in main context: {}", e2.getMessage());
                broker = null;
            }
        } catch (RuntimeException e) {
            log.debug("Broker lookup threw unexpected exception in main context: {}", e.getMessage());
            broker = null;
        }
        if (broker != null) {
            return broker;
        }

        try {
            return HstServices.getComponentManager().getComponent(ResourceServiceBroker.class, CRISP_HST_ADDON_MODULE);
        } catch (ComponentsException e) {
            log.debug("Typed broker lookup failed in addon context '{}', trying by name: {}", CRISP_HST_ADDON_MODULE, e.getMessage());
            try {
                return ResourceServiceBroker.class.cast(
                        HstServices.getComponentManager().getComponent(
                                ResourceServiceBroker.class.getName(), CRISP_HST_ADDON_MODULE));
            } catch (RuntimeException e2) {
                log.debug("Bean-name broker lookup failed in addon context '{}': {}", CRISP_HST_ADDON_MODULE, e2.getMessage());
                return null;
            }
        } catch (RuntimeException e) {
            log.debug("Broker lookup threw unexpected exception in addon context '{}': {}", CRISP_HST_ADDON_MODULE, e.getMessage());
            return null;
        }
    }
}
