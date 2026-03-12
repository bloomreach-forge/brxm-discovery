package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoverySettings;

import javax.jcr.Session;

public interface DiscoveryConfigProvider {

    DiscoveryConfig get();

    DiscoveryConfig get(Session session);

    DiscoverySettings settings();

    DiscoverySettings settings(Session session);

    void invalidate();

    void invalidateAll();
}
