package org.bloomreach.forge.discovery.transport;

import org.bloomreach.forge.discovery.exception.SearchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * {@link DiscoveryTransport} backed by {@link HttpClient}.
 * Intended to be a Spring-managed singleton with {@code destroy-method="close"}.
 */
public final class HttpClientDiscoveryTransport implements DiscoveryTransport {

    private static final Logger log = LoggerFactory.getLogger(HttpClientDiscoveryTransport.class);

    private final HttpClient httpClient;

    public HttpClientDiscoveryTransport(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String execute(DiscoveryTransportRequest request) {
        HttpRequest httpRequest = buildRequest(request);
        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.body();
            }
            String snippet = response.body() != null && response.body().length() > 200
                    ? response.body().substring(0, 200) + "..."
                    : response.body();
            throw new SearchException("Discovery API returned HTTP " + status + ": " + snippet, status);
        } catch (SearchException e) {
            throw e;
        } catch (IOException e) {
            throw new SearchException("Discovery API I/O error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SearchException("Discovery API request interrupted", e);
        }
    }

    private static HttpRequest buildRequest(DiscoveryTransportRequest request) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(request.uri())
                .GET()
                .timeout(request.timeout());
        request.headers().forEach(builder::header);
        return builder.build();
    }
}
