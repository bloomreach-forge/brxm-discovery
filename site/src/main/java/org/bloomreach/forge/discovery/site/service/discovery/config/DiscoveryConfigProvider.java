package org.bloomreach.forge.discovery.site.service.discovery.config;

import org.bloomreach.forge.discovery.site.service.discovery.config.model.DiscoveryConfig;

public interface DiscoveryConfigProvider {

    /**
     * Returns a {@link DiscoveryConfig} for the given JCR config path.
     * Null or blank path resolves via env/sys properties and coded defaults (no JCR).
     * Results are cached JVM-lifetime; use {@link #invalidate} to force re-resolution.
     */
    DiscoveryConfig get(String configPath);

    /** Removes the cache entry for a single config path, causing the next {@link #get} to re-resolve. */
    void invalidate(String configPath);

    /** Clears all cached entries. */
    void invalidateAll();
}
