package org.bloomreach.forge.discovery.cms.picker;

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import jakarta.ws.rs.InternalServerErrorException;
import org.apache.cxf.jaxrs.JAXRSInvoker;
import org.bloomreach.forge.discovery.cms.rest.DiscoveryPickerResource;
import org.bloomreach.forge.discovery.site.service.discovery.config.DiscoveryConfigReader;
import org.onehippo.repository.jaxrs.CXFRepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsEndpoint;
import org.onehippo.repository.jaxrs.RepositoryJaxrsService;
import org.onehippo.repository.modules.DaemonModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
 * {@code /hippo:configuration/hippo:modules/brxm-discovery-picker}
 */
public class DiscoveryPickerModule implements DaemonModule {

    private static final Logger log = LoggerFactory.getLogger(DiscoveryPickerModule.class);

    static final String ENDPOINT_ADDRESS = "/discovery/picker";

    private HttpClient httpClient;

    @Override
    public void initialize(Session session) throws RepositoryException {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        Function<String, String> httpGateway = url -> {
            log.debug("brxm-discovery picker → GET {}", url);
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(10))
                        .header("Accept", "application/json")
                        .GET()
                        .build();
                HttpResponse<String> response =
                        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    log.error("brxm-discovery picker ← HTTP {} for URL: {}\nResponse body: {}",
                            response.statusCode(), url, response.body());
                    throw new InternalServerErrorException(
                            "Discovery API returned HTTP " + response.statusCode());
                }
                log.debug("brxm-discovery picker ← HTTP {} ({} bytes)", response.statusCode(),
                        response.body().length());
                return response.body();
            } catch (InternalServerErrorException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InternalServerErrorException("Discovery API request interrupted");
            } catch (IOException e) {
                log.error("brxm-discovery picker request failed for URL: {}", url, e);
                throw new InternalServerErrorException(
                        "Discovery API request failed: " + e.getMessage());
            }
        };

        DiscoveryPickerResource resource = new DiscoveryPickerResource(
                session, new DiscoveryConfigReader(), httpGateway);

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
        log.info("brxm-discovery: unregistered picker endpoint");
    }
}
