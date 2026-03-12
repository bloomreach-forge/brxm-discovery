package org.bloomreach.forge.discovery.cms.rest;

import jakarta.ws.rs.InternalServerErrorException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;
import java.util.function.Function;

/**
 * Thin HTTP transport for CMS picker requests.
 * Centralizes timeout/error behavior without coupling the resource to HttpClient.
 */
public final class DiscoveryHttpGateway implements Function<String, String> {

    private final HttpClient httpClient;

    public DiscoveryHttpGateway(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient);
    }

    @Override
    public String apply(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new InternalServerErrorException("Discovery API returned HTTP " + response.statusCode());
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InternalServerErrorException("Discovery API request interrupted", e);
        } catch (IOException e) {
            throw new InternalServerErrorException("Discovery API request failed: " + e.getMessage(), e);
        }
    }
}
