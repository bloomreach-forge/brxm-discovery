package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;

/**
 * Resolves {@link PixelFlags} and pixel region from an HST request and channel configuration.
 * All methods are pure functions; no state.
 */
public final class PixelFlagsResolver {

    private PixelFlagsResolver() {
    }

    public static PixelFlags resolvePixelFlags(HstRequest request) {
        if (!PixelFlags.envEnabled()) {
            return PixelFlags.DISABLED;
        }
        Mount mount = request.getRequestContext().getResolvedMount().getMount();
        DiscoveryChannelInfo channelInfo = mount.getChannelInfo();
        String region = resolvePixelRegion(channelInfo);
        if (channelInfo == null) {
            return new PixelFlags(true, PixelFlags.envTestData(), PixelFlags.envDebug(), region);
        }
        if (!channelInfo.getDiscoveryPixelsEnabled()) {
            return PixelFlags.DISABLED;
        }
        return new PixelFlags(true, channelInfo.getDiscoveryPixelTestData(), channelInfo.getDiscoveryPixelDebug(), region);
    }

    public static String resolvePixelRegion(DiscoveryChannelInfo channelInfo) {
        String sysProp = System.getProperty("brxdis.pixel.region");
        if (sysProp != null && !sysProp.isBlank()) {
            return sysProp.toUpperCase();
        }
        if (channelInfo != null) {
            String channelRegion = channelInfo.getPixelRegion();
            if (channelRegion != null && !channelRegion.isBlank()) {
                return channelRegion.toUpperCase();
            }
        }
        return "US";
    }
}
