package org.bloomreach.forge.discovery.rest;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.UriInfo;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.model.DiscoveryConfig;
import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;

/**
 * Base class for all brXM Discovery JAX-RS resources.
 *
 * <p>Subclasses receive a shared {@link DiscoveryConfigProvider} via constructor injection
 * and inherit helpers for config resolution, credential validation, and cookie extraction.
 * JAX-RS {@code @Context} fields ({@link UriInfo}, {@link HttpServletRequest}) are injected
 * per-request by the CXF container.
 */
public abstract class AbstractDiscoveryResource {

    private static final String BR_UID_2_COOKIE = "_br_uid_2";

    private final DiscoveryConfigProvider configProvider;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected HttpServletRequest servletRequest;

    protected AbstractDiscoveryResource(DiscoveryConfigProvider configProvider) {
        this.configProvider = configProvider;
    }

    /**
     * Returns the current global Discovery configuration.
     * Never returns {@code null}; throws {@link ConfigurationException} if the config is absent.
     */
    protected DiscoveryConfig resolveConfig() {
        DiscoveryConfig config = configProvider.get();
        if (config == null) {
            throw new ConfigurationException("Discovery configuration is not available");
        }
        return config;
    }

    /**
     * Validates that the credentials in {@code config} have the minimum required fields
     * (non-blank {@code accountId} and {@code domainKey}) and returns them.
     *
     * @throws ConfigurationException if either required field is blank
     */
    protected DiscoveryCredentials requireCredentials(DiscoveryConfig config) {
        DiscoveryCredentials creds = config.credentials();
        if (creds.accountId() == null || creds.accountId().isBlank()) {
            throw new ConfigurationException("Discovery account_id is not configured");
        }
        if (creds.domainKey() == null || creds.domainKey().isBlank()) {
            throw new ConfigurationException("Discovery domain_key is not configured");
        }
        return creds;
    }

    /**
     * Extracts the {@code _br_uid_2} tracking cookie value from the inbound request.
     * Returns {@code null} if the cookie is absent.
     */
    protected String brUid2(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (BR_UID_2_COOKIE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
