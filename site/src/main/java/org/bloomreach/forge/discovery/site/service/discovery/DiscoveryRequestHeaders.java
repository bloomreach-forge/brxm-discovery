package org.bloomreach.forge.discovery.site.service.discovery;

import org.bloomreach.forge.discovery.config.model.DiscoveryCredentials;
import org.bloomreach.forge.discovery.transport.DiscoveryTransportRequest;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/** Builds {@link DiscoveryTransportRequest} objects for each Discovery API endpoint. */
public final class DiscoveryRequestHeaders {

    private static final String HEADER_AUTH_KEY = "auth-key";
    private static final Duration PIXEL_TIMEOUT = Duration.ofSeconds(5);

    private DiscoveryRequestHeaders() {
    }

    /**
     * Combines {@code baseUri} with a relative path that may contain unencoded RFC 3986 characters
     * (e.g. {@code [}, {@code ]}, spaces in {@code efq} or {@code fq} query params).
     * Uses Spring's {@link org.springframework.web.util.UriComponentsBuilder} to encode properly.
     */
    public static URI buildUri(String baseUri, String relativePath) {
        return UriComponentsBuilder
                .fromUriString(baseUri + relativePath)
                .build(false).encode().toUri();
    }

    public static DiscoveryTransportRequest forSearch(URI uri, ClientContext ctx) {
        return DiscoveryTransportRequest.of(uri, commonHeaders(ctx));
    }

    public static DiscoveryTransportRequest forPathways(URI uri, DiscoveryCredentials credentials, ClientContext ctx) {
        Map<String, String> headers = commonHeaders(ctx);
        if (credentials.authKey() != null && !credentials.authKey().isBlank()) {
            headers.put(HEADER_AUTH_KEY, credentials.authKey());
        }
        return DiscoveryTransportRequest.of(uri, headers);
    }

    public static DiscoveryTransportRequest forPixel(URI uri, ClientContext ctx) {
        return DiscoveryTransportRequest.of(uri, commonHeaders(ctx), PIXEL_TIMEOUT);
    }

    private static Map<String, String> commonHeaders(ClientContext ctx) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        if (isForwardClientHeaders() && ctx != null) {
            applyClientHeaders(headers, ctx);
        }
        return headers;
    }

    private static boolean isForwardClientHeaders() {
        return Boolean.parseBoolean(System.getProperty("brxdis.forwardClientHeaders", "true"));
    }

    private static void applyClientHeaders(Map<String, String> headers, ClientContext ctx) {
        if (notBlank(ctx.userAgent()))       headers.put("User-Agent", ctx.userAgent());
        if (notBlank(ctx.acceptLanguage()))  headers.put("Accept-Language", ctx.acceptLanguage());
        if (notBlank(ctx.xForwardedFor()))   headers.put("X-Forwarded-For", ctx.xForwardedFor());
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
