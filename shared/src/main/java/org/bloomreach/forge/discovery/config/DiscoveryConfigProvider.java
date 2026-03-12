package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;

import javax.jcr.Session;

public interface DiscoveryConfigProvider {

    DiscoveryConfig get();

    DiscoveryConfig get(Session session);

    void invalidate();

    void invalidateAll();
}
