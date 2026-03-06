package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.exception.ConfigurationException;
import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class DiscoveryConfigResolver {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryConfigResolver.class);
    public static final String CONFIG_PATH_PARAM = "discoveryConfigPath";

    private final DiscoveryConfigReader configReader;

    public DiscoveryConfigResolver(DiscoveryConfigReader configReader) {
        this.configReader = configReader;
    }

    /**
     * Resolves a {@link DiscoveryConfig} with graceful degradation:
     * <ol>
     *   <li>If {@code configPath} is set and JCR node exists → full two-tier resolution</li>
     *   <li>If {@code configPath} is null/blank → env/sys credentials + coded defaults</li>
     *   <li>If {@code configPath} is set but node missing → log warning, fallback to env/sys + defaults</li>
     * </ol>
     * Throws {@link ConfigurationException} only when required credentials are missing from ALL sources.
     */
    public DiscoveryConfig resolve(Session session, String configPath) {
        DiscoveryConfig config;

        if (configPath == null || configPath.isBlank()) {
            log.debug("No {} configured — resolving config from env/sys properties and coded defaults",
                    CONFIG_PATH_PARAM);
            config = configReader.readWithDefaults();
        } else {
            try {
                Node configNode = session.getNode(configPath);
                config = configReader.read(configNode);
            } catch (PathNotFoundException e) {
                log.warn("Discovery config node not found at '{}' — falling back to env/sys and defaults",
                        configPath);
                config = configReader.readWithDefaults();
            } catch (RepositoryException e) {
                log.error("Failed to read Discovery config from {}: {}", configPath, e.getMessage());
                throw new ConfigurationException(
                        "Failed to read Discovery config from '" + configPath + "': " + e.getMessage(), e);
            }
        }

        validateRequiredCredentials(config);
        return config;
    }

    private static void validateRequiredCredentials(DiscoveryConfig config) {
        if (isBlank(config.accountId())) {
            throw new ConfigurationException(
                    "Discovery accountId is required — set BRXDIS_ACCOUNT_ID env var, -Dbrxdis.accountId, or brxdis:accountId JCR property");
        }
        if (isBlank(config.domainKey())) {
            throw new ConfigurationException(
                    "Discovery domainKey is required — set BRXDIS_DOMAIN_KEY env var, -Dbrxdis.domainKey, or brxdis:domainKey JCR property");
        }
        if (isBlank(config.apiKey())) {
            throw new ConfigurationException(
                    "Discovery apiKey is required — set BRXDIS_API_KEY env var, -Dbrxdis.apiKey, or brxdis:apiKey JCR property");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
