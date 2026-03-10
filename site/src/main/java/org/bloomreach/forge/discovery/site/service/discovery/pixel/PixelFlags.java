package org.bloomreach.forge.discovery.site.service.discovery.pixel;

/**
 * Carries per-request pixel control flags resolved from env/system properties and channel parameters.
 * <p>
 * {@link #DISABLED} is the safe constant for fire methods that must not produce any pixel traffic
 * (e.g., test contexts, CMS-preview requests).
 */
public record PixelFlags(boolean enabled, boolean testData, boolean debug, String region) {

    public static final PixelFlags DISABLED = new PixelFlags(false, false, false, "US");

    /** Env/system kill switch — defaults to {@code true}. Set {@code brxdis.pixel.envEnabled=false} to disable globally. */
    public static boolean envEnabled() {
        return Boolean.parseBoolean(System.getProperty("brxdis.pixel.envEnabled", "true"));
    }

    /** Env/system test_data flag — defaults to {@code false}. Set {@code brxdis.pixel.testData=true} for non-production traffic. */
    public static boolean envTestData() {
        return Boolean.parseBoolean(System.getProperty("brxdis.pixel.testData", "false"));
    }

    /** Env/system debug flag — defaults to {@code false}. Set {@code brxdis.pixel.debug=true} to append debug param. */
    public static boolean envDebug() {
        return Boolean.parseBoolean(System.getProperty("brxdis.pixel.debug", "false"));
    }

    /** Pixel region — returns {@code "EU"} or {@code "US"} (default). Set {@code brxdis.pixel.region=EU} for EU endpoint. */
    public static String pixelRegion() {
        return System.getProperty("brxdis.pixel.region", "US").toUpperCase();
    }
}
