package org.bloomreach.forge.discovery.config;

import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class DiscoveryConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigResolver.class);

    private final DiscoveryConfigReader configReader;

    public DiscoveryConfigResolver(DiscoveryConfigReader configReader) {
        this.configReader = configReader;
    }

    public DiscoveryConfig resolve(Session session) {
        try {
            Node configNode = session.getNode(ConfigDefaults.CONFIG_NODE_PATH);
            return configReader.read(configNode);
        } catch (PathNotFoundException e) {
            log.warn("Discovery config node not found at '{}' — falling back to env/sys and defaults",
                    ConfigDefaults.CONFIG_NODE_PATH);
            return configReader.readWithDefaults();
        } catch (RepositoryException e) {
            log.error("Failed to read Discovery config from {}: {}", ConfigDefaults.CONFIG_NODE_PATH, e.getMessage());
            throw new ConfigurationException(
                    "Failed to read Discovery config from '" + ConfigDefaults.CONFIG_NODE_PATH + "': " + e.getMessage(), e);
        }
    }

    public DiscoveryConfig resolveDefaults() {
        return configReader.readWithDefaults();
    }

    public DiscoveryConfig applyEnvSysCredentials(DiscoveryConfig base) {
        return base.withCredentialOverrides(configReader.credentialsFromEnvSysOnly());
    }
}
