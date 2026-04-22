package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.exception.ConfigurationException;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.api.resource.ResourceException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;

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
        try {
            return broker().resolve(resourceSpace, path, DiscoveryExchangeHints.buildHint(ctx));
        } catch (ResourceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unwrapProxyException(e);
        }
    }

    Resource resolvePathways(String resourceSpace, String path, DiscoveryCredentials credentials,
                             ClientContext ctx) throws ResourceException {
        try {
            return broker().resolve(resourceSpace, path, DiscoveryExchangeHints.buildV2Hint(credentials, ctx));
        } catch (ResourceException e) {
            throw e;
        } catch (RuntimeException e) {
            throw unwrapProxyException(e);
        }
    }

    /**
     * Unwraps proxy-layer wrappers (UndeclaredThrowableException, InvocationTargetException)
     * that Spring AOP / JDK proxies may add around the real cause. If the root cause is already
     * a ResourceException it is rethrown as-is so callers' existing catch blocks stay intact.
     */
    private static RuntimeException unwrapProxyException(RuntimeException e) {
        Throwable t = e;
        while (t instanceof UndeclaredThrowableException u) {
            Throwable inner = u.getUndeclaredThrowable();
            t = inner != null ? inner : t;
        }
        while (t instanceof InvocationTargetException ite) {
            Throwable inner = ite.getCause();
            t = inner != null ? inner : ite;
        }
        if (t instanceof ResourceException re) return re;
        if (t instanceof RuntimeException re) return re;
        return new ConfigurationException("CRISP proxy invocation failed: " + t.getMessage(), t);
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
