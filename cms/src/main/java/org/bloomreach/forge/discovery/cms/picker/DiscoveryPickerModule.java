package org.bloomreach.forge.discovery.cms.picker;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.bloomreach.forge.discovery.cms.rest.DiscoveryHttpGateway;
import org.bloomreach.forge.discovery.cms.rest.DiscoveryPickerResource;
import org.bloomreach.forge.discovery.config.CachingDiscoveryConfigProvider;
import org.bloomreach.forge.discovery.config.DiscoveryConfigJcrListener;
import org.bloomreach.forge.discovery.config.DiscoveryConfigReader;
import org.bloomreach.forge.discovery.config.DiscoveryConfigResolver;
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
    private static final String PIXEL_CRISP_NODE =
            "/hippo:configuration/hippo:modules/crispregistry/" +
            "hippo:moduleconfig/crisp:resourceresolvercontainer/discoveryPixelAPI";

    private final Function<String, String> envLookup;
    private HttpClient httpClient;
    private DiscoveryConfigProvider configProvider;
    private DiscoveryConfigJcrListener configListener;

    public DiscoveryPickerModule() {
        this.envLookup = System::getenv;
    }

    /** Package-private seam for tests. */
    DiscoveryPickerModule(Function<String, String> envLookup) {
        this.envLookup = envLookup;
    }

    @Override
    public void initialize(Session session) throws RepositoryException {
        DiscoveryConfigResolver configResolver = new DiscoveryConfigResolver(new DiscoveryConfigReader());
        if (applyPixelBaseUriOverride(session)) {
            session.save();
        }

        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Function<String, String> httpGateway = new DiscoveryHttpGateway(httpClient);

        CachingDiscoveryConfigProvider localConfigProvider =
                new CachingDiscoveryConfigProvider(configResolver);
        configProvider = localConfigProvider;
        registerConfigProvider(configProvider);
        configListener = new DiscoveryConfigJcrListener(localConfigProvider);
        configListener.start();

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

    boolean applyPixelBaseUriOverride(Session session) {
        String override = envLookup.apply("BRXDIS_PIXEL_BASEURI");
        if (override == null || override.isBlank()) {
            override = System.getProperty("brxdis.pixelBaseUri");
        }
        if (override == null || override.isBlank()) {
            return false;
        }

        try {
            if (!session.nodeExists(PIXEL_CRISP_NODE)) {
                log.warn("brxm-discovery: discoveryPixelAPI CRISP node not found — pixel base URI override skipped");
                return false;
            }
            session.getNode(PIXEL_CRISP_NODE).setProperty("crisp:propvalues", new String[]{override});
            log.info("brxm-discovery: pixel base URI set to {} (env/sys override)", override);
            return true;
        } catch (RepositoryException e) {
            log.warn("brxm-discovery: failed to apply pixel base URI override", e);
            return false;
        }
    }

    @Override
    public void shutdown() {
        RepositoryJaxrsService.removeEndpoint(ENDPOINT_ADDRESS);
        if (configListener != null) {
            configListener.close();
            configListener = null;
        }
        if (configProvider != null) {
            unregisterConfigProvider(configProvider);
            configProvider = null;
        }
        log.info("brxm-discovery: unregistered picker endpoint");
    }

    void registerConfigProvider(DiscoveryConfigProvider provider) {
        HippoServiceRegistry.registerService(provider, DiscoveryConfigProvider.class);
    }

    void unregisterConfigProvider(DiscoveryConfigProvider provider) {
        HippoServiceRegistry.unregisterService(provider, DiscoveryConfigProvider.class);
    }
}
