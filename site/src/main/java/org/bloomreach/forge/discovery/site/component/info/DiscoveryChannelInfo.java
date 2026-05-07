package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * Channel Manager parameters for Discovery credential overrides and pixel tracking.
 */
@FieldGroupList({
    @FieldGroup(
        value = {"discoveryAccountId", "discoveryDomainKey", "discoveryApiKeyEnvVar", "discoveryAuthKeyEnvVar"},
        titleKey = "Credentials"
    ),
    @FieldGroup(
        value = {"discoveryDefaultFieldList", "discoveryCatalogName"},
        titleKey = "Schema"
    ),
    @FieldGroup(
        value = {"discoveryPixelsEnabled", "discoveryPixelConsentCookie", "discoveryPixelTestData", "discoveryPixelDebug", "discoveryPixelRegion"},
        titleKey = "Pixel Tracking"
    ),
    @FieldGroup(
        value = {"discoveryVisualSearchEnabled", "discoveryVisualSearchWidgetId"},
        titleKey = "Visual Search"
    )
})
public interface DiscoveryChannelInfo extends ChannelInfo {

    @Parameter(name = "discoveryAccountId", displayName = "Account ID override", defaultValue = "")
    String getDiscoveryAccountId();

    @Parameter(name = "discoveryDomainKey", displayName = "Domain Key override", defaultValue = "")
    String getDiscoveryDomainKey();

    @Parameter(name = "discoveryApiKeyEnvVar", displayName = "API Key env-var name", defaultValue = "")
    String getDiscoveryApiKeyEnvVar();

    @Parameter(name = "discoveryAuthKeyEnvVar", displayName = "Auth Key env-var name (v2/Pathways)", defaultValue = "")
    String getDiscoveryAuthKeyEnvVar();

    /**
     * Comma-separated Discovery {@code fl} field list for this channel.
     * Replaces the global default when set. Leave blank to use the global JCR default or the
     * coded default ({@code pid,title,thumb_image,url,price,brand,sale_price,description}).
     * Different channels pointing at different Discovery accounts/catalogs can declare their
     * own field sets here.
     */
    @Parameter(name = "discoveryDefaultFieldList", displayName = "Field list (fl) override", defaultValue = "")
    String getDiscoveryDefaultFieldList();

    @Parameter(name = "discoveryCatalogName", displayName = "Catalog name", defaultValue = "")
    String getDiscoveryCatalogName();

    @Parameter(name = "discoveryPixelsEnabled", displayName = "Send pixel events", defaultValue = "true")
    boolean getDiscoveryPixelsEnabled();

    /**
     * Name of the HTTP cookie that signals user consent to pixel tracking.
     * Leave blank to fire pixels unconditionally (default). When set, pixels are
     * suppressed unless the named cookie is present on the request.
     * Override with a {@code brxmdis.pixelConsentProvider} Spring bean for complex CMPs.
     */
    @Parameter(name = "discoveryPixelConsentCookie", displayName = "Consent cookie name", defaultValue = "")
    String getDiscoveryPixelConsentCookie();

    @Parameter(name = "discoveryPixelTestData", displayName = "Mark pixels as test data", defaultValue = "false")
    boolean getDiscoveryPixelTestData();

    @Parameter(name = "discoveryPixelDebug", displayName = "Pixel debug mode", defaultValue = "false")
    boolean getDiscoveryPixelDebug();

    @Parameter(name = "discoveryPixelRegion", displayName = "Pixel region", defaultValue = "US")
    @DropDownList({"US", "EU"})
    String getPixelRegion();

    @Parameter(name = "discoveryVisualSearchEnabled", displayName = "Enable visual search", defaultValue = "false")
    boolean getDiscoveryVisualSearchEnabled();

    @Parameter(name = "discoveryVisualSearchWidgetId", displayName = "Visual search widget ID", defaultValue = "")
    String getDiscoveryVisualSearchWidgetId();
}
