package org.bloomreach.forge.discovery.site.service.discovery.pixel;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PixelFlagsTest {

    @Test
    void envEnabled_defaultTrue() {
        // No system property set → default "true"
        System.clearProperty("brxdis.pixel.envEnabled");
        assertTrue(PixelFlags.envEnabled());
    }

    @Test
    void envTestData_defaultFalse() {
        System.clearProperty("brxdis.pixel.testData");
        assertFalse(PixelFlags.envTestData());
    }

    @Test
    void envDebug_defaultFalse() {
        System.clearProperty("brxdis.pixel.debug");
        assertFalse(PixelFlags.envDebug());
    }

    @Test
    void disabled_constant_hasAllFalse() {
        assertFalse(PixelFlags.DISABLED.enabled());
        assertFalse(PixelFlags.DISABLED.testData());
        assertFalse(PixelFlags.DISABLED.debug());
    }

    @Test
    void region_fieldStoredAndReturned() {
        PixelFlags flags = new PixelFlags(true, false, false, "EU");
        assertEquals("EU", flags.region());
    }

    @Test
    void region_disabled_defaultsToUS() {
        assertEquals("US", PixelFlags.DISABLED.region());
    }

    @Test
    void pixelRegion_defaultUS() {
        System.clearProperty("brxdis.pixel.region");
        assertEquals("US", PixelFlags.pixelRegion());
    }

    @Test
    void pixelRegion_whenSetToEU_returnsEU() {
        System.setProperty("brxdis.pixel.region", "EU");
        try {
            assertEquals("EU", PixelFlags.pixelRegion());
        } finally {
            System.clearProperty("brxdis.pixel.region");
        }
    }

    @Test
    void pixelRegion_lowercaseInput_normalizedToUppercase() {
        System.setProperty("brxdis.pixel.region", "eu");
        try {
            assertEquals("EU", PixelFlags.pixelRegion());
        } finally {
            System.clearProperty("brxdis.pixel.region");
        }
    }
}
