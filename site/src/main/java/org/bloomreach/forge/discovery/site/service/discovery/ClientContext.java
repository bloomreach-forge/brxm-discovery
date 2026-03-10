package org.bloomreach.forge.discovery.site.service.discovery;

/**
 * Carries browser identity headers extracted from an incoming HST request.
 * Passed as a plain record so no {@code HstRequest} reference escapes into async pixel dispatch.
 */
public record ClientContext(String userAgent, String acceptLanguage, String xForwardedFor) {
    public static final ClientContext EMPTY = new ClientContext(null, null, null);
}
