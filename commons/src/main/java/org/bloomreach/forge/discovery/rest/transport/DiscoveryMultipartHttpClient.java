package org.bloomreach.forge.discovery.rest.transport;

import org.bloomreach.forge.discovery.exception.SearchException;

import java.io.InputStream;

/**
 * HTTP client for Discovery Pathways endpoints.
 * Supports both multipart POST (image upload) and plain GET (search).
 * Separated as an interface so that site-layer resources can be tested without real HTTP.
 */
public interface DiscoveryMultipartHttpClient {

    /**
     * Uploads an image to the given URL and returns the raw JSON response body.
     *
     * @param url         fully-qualified upload URL (credentials already in query string)
     * @param imageStream image bytes; must be less than 2 MB
     * @param contentType MIME type; must start with {@code image/}
     * @return raw JSON response body from Discovery
     * @throws SearchException if validation fails, the HTTP call fails, or a non-2xx is returned
     */
    String upload(String url, InputStream imageStream, String contentType) throws SearchException;

    /**
     * Issues a plain GET to the given URL and returns the raw JSON response body.
     *
     * @param url fully-qualified URL (credentials already in query string)
     * @return raw JSON response body from Discovery
     * @throws SearchException if the HTTP call fails or a non-2xx is returned
     */
    String get(String url) throws SearchException;
}
