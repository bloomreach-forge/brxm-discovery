package org.bloomreach.forge.discovery.site.platform;

import org.bloomreach.forge.discovery.site.component.info.DiscoveryChannelInfo;
import org.bloomreach.forge.discovery.site.service.discovery.pixel.PixelFlags;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PixelFlagsResolverTest {

    @Mock HstRequest request;
    @Mock HstRequestContext requestContext;
    @Mock ResolvedMount resolvedMount;
    @Mock Mount mount;
    @Mock DiscoveryChannelInfo channelInfo;

    @BeforeEach
    void setUpRequest() {
        lenient().when(request.getRequestContext()).thenReturn(requestContext);
        lenient().when(requestContext.getResolvedMount()).thenReturn(resolvedMount);
        lenient().when(resolvedMount.getMount()).thenReturn(mount);
        lenient().when(mount.getChannelInfo()).thenReturn(channelInfo);
        lenient().when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(true);
        lenient().when(channelInfo.getDiscoveryPixelTestData()).thenReturn(false);
        lenient().when(channelInfo.getDiscoveryPixelDebug()).thenReturn(false);
    }

    @Test
    void resolvePixelRegion_nullChannelInfo_defaultsToUS() {
        System.clearProperty("brxdis.pixel.region");
        assertEquals("US", PixelFlagsResolver.resolvePixelRegion(null));
    }

    @Test
    void resolvePixelRegion_channelInfoEU_returnsEU() {
        System.clearProperty("brxdis.pixel.region");
        when(channelInfo.getPixelRegion()).thenReturn("EU");
        assertEquals("EU", PixelFlagsResolver.resolvePixelRegion(channelInfo));
    }

    @Test
    void resolvePixelRegion_sysPropWinsOverChannelInfo() {
        System.setProperty("brxdis.pixel.region", "EU");
        try {
            assertEquals("EU", PixelFlagsResolver.resolvePixelRegion(channelInfo));
        } finally {
            System.clearProperty("brxdis.pixel.region");
        }
    }

    @Test
    void resolvePixelRegion_channelInfoLowercase_normalizedToUppercase() {
        System.clearProperty("brxdis.pixel.region");
        when(channelInfo.getPixelRegion()).thenReturn("eu");
        assertEquals("EU", PixelFlagsResolver.resolvePixelRegion(channelInfo));
    }

    // ── resolvePixelFlags ─────────────────────────────────────────────────

    @Test
    void resolvePixelFlags_envDisabled_returnsDisabled() {
        System.setProperty("brxdis.pixel.envEnabled", "false");
        try {
            assertEquals(PixelFlags.DISABLED, PixelFlagsResolver.resolvePixelFlags(request));
        } finally {
            System.clearProperty("brxdis.pixel.envEnabled");
        }
    }

    @Test
    void resolvePixelFlags_nullChannelInfo_usesEnvDefaults() {
        System.clearProperty("brxdis.pixel.region");
        when(mount.getChannelInfo()).thenReturn(null);

        PixelFlags flags = PixelFlagsResolver.resolvePixelFlags(request);

        assertTrue(flags.enabled());
        assertFalse(flags.testData());
        assertFalse(flags.debug());
        assertEquals("US", flags.region());
    }

    @Test
    void resolvePixelFlags_channelPixelsDisabled_returnsDisabled() {
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(false);

        assertEquals(PixelFlags.DISABLED, PixelFlagsResolver.resolvePixelFlags(request));
    }

    @Test
    void resolvePixelFlags_channelPixelsEnabled_returnsChannelFlags() {
        System.clearProperty("brxdis.pixel.region");
        when(channelInfo.getDiscoveryPixelsEnabled()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelTestData()).thenReturn(true);
        when(channelInfo.getDiscoveryPixelDebug()).thenReturn(true);
        when(channelInfo.getPixelRegion()).thenReturn("EU");

        PixelFlags flags = PixelFlagsResolver.resolvePixelFlags(request);

        assertTrue(flags.enabled());
        assertTrue(flags.testData());
        assertTrue(flags.debug());
        assertEquals("EU", flags.region());
    }
}
