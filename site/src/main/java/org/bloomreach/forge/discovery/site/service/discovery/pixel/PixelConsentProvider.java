package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.hippoecm.hst.core.component.HstRequest;

/**
 * SPI for custom pixel tracking consent resolution.
 *
 * <p>Register an implementation as a Spring bean named {@code brxmdis.pixelConsentProvider}
 * in your site assembly to override the default cookie-based consent check. If no bean is
 * registered, the plugin falls back to checking the cookie named by
 * {@code discoveryPixelConsentCookie} on the channel info. If that is also blank, pixels
 * fire unconditionally.
 *
 * <p>Example - OneTrust analytics category:
 * <pre>{@code
 * @Component("brxmdis.pixelConsentProvider")
 * public class OneTrustPixelConsentProvider implements PixelConsentProvider {
 *     @Override
 *     public boolean hasConsent(HstRequest request) {
 *         String otz = CookieUtils.getCookieValue(request, "OptanonConsent");
 *         return otz != null && otz.contains("C0002%3A1");
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface PixelConsentProvider {
    boolean hasConsent(HstRequest request);
}
