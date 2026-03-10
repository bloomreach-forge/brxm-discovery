package org.bloomreach.forge.discovery.site.component.info;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.FieldGroup;
import org.hippoecm.hst.core.parameters.FieldGroupList;
import org.hippoecm.hst.core.parameters.Parameter;

/**
 * Channel Manager parameters for controlling Discovery credentials, pixel tracking, and region at mount level.
 */
@FieldGroupList({
    @FieldGroup(value = {"discoveryPixelsEnabled", "discoveryPixelTestData", "discoveryPixelDebug", "discoveryPixelRegion"}, titleKey = "Pixel Tracking"),
    @FieldGroup(value = {"discoveryAccountId", "discoveryDomainKey", "discoveryApiKeyEnvVar", "discoveryAuthKeyEnvVar"}, titleKey = "Discovery Credentials")
})
public interface DiscoveryChannelInfo extends ChannelInfo {

    @Parameter(name = "discoveryPixelsEnabled", displayName = "Send pixel events", defaultValue = "true")
    boolean getDiscoveryPixelsEnabled();

    @Parameter(name = "discoveryPixelTestData", displayName = "Mark pixels as test data", defaultValue = "false")
    boolean getDiscoveryPixelTestData();

    @Parameter(name = "discoveryPixelDebug", displayName = "Pixel debug mode", defaultValue = "false")
    boolean getDiscoveryPixelDebug();

    @Parameter(name = "discoveryPixelRegion", displayName = "Pixel region", defaultValue = "US")
    @DropDownList({"US", "EU"})
    String getPixelRegion();

    @Parameter(name = "discoveryAccountId", displayName = "Account ID", defaultValue = "")
    String getAccountId();

    @Parameter(name = "discoveryDomainKey", displayName = "Domain key", defaultValue = "")
    String getDomainKey();

    @Parameter(name = "discoveryApiKeyEnvVar", displayName = "API key env variable", defaultValue = "")
    String getApiKeyEnvVar();

    @Parameter(name = "discoveryAuthKeyEnvVar", displayName = "Auth key env variable", defaultValue = "")
    String getAuthKeyEnvVar();
}
