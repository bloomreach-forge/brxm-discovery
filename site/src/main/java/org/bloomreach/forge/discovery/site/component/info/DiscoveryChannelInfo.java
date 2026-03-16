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
        value = {"discoveryPixelsEnabled", "discoveryPixelTestData", "discoveryPixelDebug", "discoveryPixelRegion"},
        titleKey = "Pixel Tracking"
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

    @Parameter(name = "discoveryPixelsEnabled", displayName = "Send pixel events", defaultValue = "true")
    boolean getDiscoveryPixelsEnabled();

    @Parameter(name = "discoveryPixelTestData", displayName = "Mark pixels as test data", defaultValue = "false")
    boolean getDiscoveryPixelTestData();

    @Parameter(name = "discoveryPixelDebug", displayName = "Pixel debug mode", defaultValue = "false")
    boolean getDiscoveryPixelDebug();

    @Parameter(name = "discoveryPixelRegion", displayName = "Pixel region", defaultValue = "US")
    @DropDownList({"US", "EU"})
    String getPixelRegion();
}
