package org.bloomreach.forge.discovery.cms.picker;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.bloomreach.forge.discovery.cms.rest.DiscoveryHttpGateway;
import org.bloomreach.forge.discovery.cms.rest.DiscoveryPickerResource;
import org.bloomreach.forge.discovery.config.CachingDiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigProvider;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Function;

/**
 * Daemon module that registers the Discovery product picker JAX-RS endpoint
 * with brXM's {@link RepositoryJaxrsService}.
 *
 * <p>The endpoint is accessible at {@code {cmsContext}/ws/discovery/picker}
 * once this module is bootstrapped via HCM config.
 *
 * <p>Bootstrap path:
 * {@code /hippo:configuration/hippo:modules/brxm-discovery}
 */
public class DiscoveryPickerModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPickerModule.class);

    static final String ENDPOINT_ADDRESS = "/discovery/picker";

    private HttpClient httpClient;
    private DiscoveryConfigProvider configProvider;

    @Override
    public void initialize(Session session) throws RepositoryException {
        DiscoveryConfigReader configReader = new DiscoveryConfigReader();

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Function<String, String> httpGateway = new DiscoveryHttpGateway(httpClient);

        CachingDiscoveryConfigProvider localConfigProvider =
                new CachingDiscoveryConfigProvider(configReader);
        localConfigProvider.start();
        configProvider = localConfigProvider;
        registerConfigProvider(configProvider);

        DiscoveryPickerResource resource = new DiscoveryPickerResource(
                session, configProvider, httpGateway);

        RepositoryJaxrsEndpoint endpoint =
                new CXFRepositoryJaxrsEndpoint(ENDPOINT_ADDRESS)
                        .invoker(new JAXRSInvoker())
                        .singleton(resource)
                        .singleton(new JacksonJsonProvider());

        RepositoryJaxrsService.addEndpoint(endpoint);
        log.info("brxm-discovery: registered picker endpoint at {}", ENDPOINT_ADDRESS);
    }

    @Override
    public void shutdown() {
        RepositoryJaxrsService.removeEndpoint(ENDPOINT_ADDRESS);
        if (configProvider instanceof CachingDiscoveryConfigProvider cachingProvider) {
            cachingProvider.close();
        }
        if (configProvider != null) {
            unregisterConfigProvider(configProvider);
            configProvider = null;
        }
        log.info("brxm-discovery: unregistered picker endpoint");
    }

    void registerConfigProvider(DiscoveryConfigProvider provider) {
        HippoServiceRegistry.register(provider, DiscoveryConfigProvider.class);
    }

    void unregisterConfigProvider(DiscoveryConfigProvider provider) {
        HippoServiceRegistry.unregister(provider, DiscoveryConfigProvider.class);
    }
}
